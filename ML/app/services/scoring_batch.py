import time
import concurrent.futures
from sqlalchemy import text
from app.config.database import SessionLocal
from app.services.scoring_service import compute_property_score
from app.utils.logger import logger

def update_property_score_optimized(property_id: int, score_data: dict, session, max_retries=3) -> bool:
    """
    DB 업데이트 (property_score 테이블). 데드락 발생 시 재시도.
    """
    from sqlalchemy import text
    retries = 0
    while retries < max_retries:
        try:
            stmt = text(
                """UPDATE property_score SET
                   transport_count = :transport_count, transport_score = :transport_score,
                   restaurant_count = :restaurant_count, restaurant_score = :restaurant_score,
                   health_count = :health_count, health_score = :health_score,
                   convenience_count = :convenience_count, convenience_score = :convenience_score,
                   cafe_count = :cafe_count, cafe_score = :cafe_score,
                   chicken_count = :chicken_count, chicken_score = :chicken_score,
                   leisure_count = :leisure_count, leisure_score = :leisure_score
                 WHERE property_id = :property_id
                """
            )
            params = {"property_id": property_id, **score_data}
            result = session.execute(stmt, params)
            if result.rowcount == 0:
                # INSERT if not exist
                insert_stmt = text(
                    """INSERT INTO property_score (
                       property_id, transport_count, transport_score,
                       restaurant_count, restaurant_score, health_count, health_score,
                       convenience_count, convenience_score, cafe_count, cafe_score,
                       chicken_count, chicken_score, leisure_count, leisure_score
                    ) VALUES (
                       :property_id, :transport_count, :transport_score,
                       :restaurant_count, :restaurant_score, :health_count, :health_score,
                       :convenience_count, :convenience_score, :cafe_count, :cafe_score,
                       :chicken_count, :chicken_score, :leisure_count, :leisure_score
                    )
                    """
                )
                session.execute(insert_stmt, params)
            return True
        except Exception as e:
            session.rollback()
            if "Deadlock found" in str(e):
                retries += 1
                logger.warning("[Deadlock] property_id=%s retry %d/%d", property_id, retries, max_retries)
                time.sleep(1)
            else:
                logger.error("[Error] update_property_score for ID=%s => %s", property_id, e)
                return False
    logger.error("[Max Retry] property_id=%s", property_id)
    return False

def process_property(row, session):
    """
    한 매물(row)에 대한 점수 계산 + DB 업데이트
    """
    prop_id = row._mapping["property_id"]
    lat = row._mapping["latitude"]
    lon = row._mapping["longitude"]
    try:
        score_data = compute_property_score({"latitude": lat, "longitude": lon}, session=session)
        if update_property_score_optimized(prop_id, score_data, session=session):
            return True
        return False
    except Exception as e:
        logger.error("[Error] process_property: ID=%s, %s", prop_id, e)
        return False

def process_property_batch(rows):
    """
    하나의 세션에서 batch 내 모든 매물을 처리.
    batch 내 오류: 해당 매물만 실패, 전체 batch는 계속 진행.
    """
    session = SessionLocal()
    success_count = 0
    try:
        for row in rows:
            if process_property(row, session):
                success_count += 1
        session.commit()
        return success_count
    except Exception as e:
        session.rollback()
        logger.error("[Batch Error] %s", e)
        return success_count
    finally:
        session.close()

def recalculate_all_scores_batch(batch_size=1000, max_workers=8, limit=None):
    """
    멀티스레드 방식 배치.
    진행률(%) 로깅 추가.
    """
    from app.services.poi_cache import initialize_poi_cache
    initialize_poi_cache()

    session = SessionLocal()
    total_processed = 0
    try:
        if limit is None:
            total_count = session.execute(text("SELECT COUNT(*) FROM property")).scalar()
        else:
            total_count_db = session.execute(text("SELECT COUNT(*) FROM property")).scalar()
            total_count = min(limit, total_count_db)

        logger.info("[Batch] Total properties to process: %d", total_count)
        offset = 0
        start_time = time.time()

        while total_processed < total_count:
            query = text("""
                SELECT property_id, latitude, longitude
                FROM property
                ORDER BY property_id
                LIMIT :batch_size OFFSET :offset
            """)
            rows = session.execute(query, {"batch_size": batch_size, "offset": offset}).fetchall()
            if not rows:
                break

            sub_batches = []
            sub_batch_size = len(rows) // max_workers if max_workers > 0 else len(rows)
            if sub_batch_size < 1:
                sub_batch_size = 1

            for i in range(0, len(rows), sub_batch_size):
                sub_batches.append(rows[i:i+sub_batch_size])

            batch_processed = 0
            with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as executor:
                futures = [executor.submit(process_property_batch, sb) for sb in sub_batches]
                for future in concurrent.futures.as_completed(futures):
                    try:
                        batch_processed += future.result()
                    except Exception as e:
                        logger.error("[Sub-batch error] %s", e)

            total_processed += batch_processed
            offset += batch_size

            pct = (total_processed / total_count) * 100.0
            elapsed = time.time() - start_time
            logger.info("[Progress] %d/%d (%.2f%%) in %.1fs", total_processed, total_count, pct, elapsed)

            if total_processed >= total_count:
                break

    except Exception as e:
        logger.error("[Batch Error] %s", e)
    finally:
        session.close()

    logger.info("[Done] Processed %d/%d", total_processed, total_count)
    return total_processed
