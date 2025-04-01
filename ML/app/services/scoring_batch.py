import time
import concurrent.futures
from sqlalchemy import text
from app.config.database import SessionLocal
from app.services.scoring_service import compute_property_score
from app.services.poi_cache import initialize_poi_cache
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
                # 존재하지 않으면 INSERT
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

            # rowcount != 0 이면 UPDATE 성공
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

    # 최대 재시도 초과
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


def recalculate_all_scores_no_batch() -> int:
    """
    [비배치] 전체 매물을 한 번에 로드 -> 단일 스레드로 순차 처리.
    예: 메모리 사용 많지만, 배치 처리 없이 간단히 진행 (테스트용).
    """
    initialize_poi_cache()

    session = SessionLocal()
    try:
        rows = session.execute(text("SELECT property_id, latitude, longitude FROM property ORDER BY property_id")).fetchall()
    finally:
        session.close()

    total_processed = 0
    processing_session = SessionLocal()
    try:
        start_time = time.time()
        for idx, row in enumerate(rows, start=1):
            if process_property(row, processing_session):
                total_processed += 1

            # 필요 시 주기적으로 commit
            if idx % 100 == 0:
                processing_session.commit()

        processing_session.commit()
        elapsed = time.time() - start_time
        logger.info("[No Batch] Processed %d properties in %.2f seconds", total_processed, elapsed)

    except Exception as e:
        processing_session.rollback()
        logger.error("[No Batch Error] %s", e)
    finally:
        processing_session.close()

    return total_processed


def recalculate_all_scores_single(limit: int = 20000, batch_size: int = 1000) -> int:
    """
    [단일 스레드 배치 방식] limit만큼 데이터를 batch_size 단위로 조회, 순차 처리.
    """
    initialize_poi_cache()

    total_processed = 0
    session = SessionLocal()
    processing_session = SessionLocal()
    try:
        offset = 0
        start_time = time.time()
        while total_processed < limit:
            query = text(
                "SELECT property_id, latitude, longitude FROM property "
                "ORDER BY property_id LIMIT :limit OFFSET :offset"
            )
            rows = session.execute(query, {"limit": batch_size, "offset": offset}).fetchall()
            if not rows:
                break

            for row in rows:
                if process_property(row, processing_session):
                    total_processed += 1

            processing_session.commit()

            offset += batch_size
            logger.info("[Single] Processed %d / %d", total_processed, limit)
            if total_processed >= limit:
                break

        elapsed = time.time() - start_time
        logger.info("[Single] Total processed %d in %.2f sec", total_processed, elapsed)

    except Exception as e:
        processing_session.rollback()
        logger.error("[Single Error] %s", e)
    finally:
        processing_session.close()
        session.close()

    return total_processed


def recalculate_all_scores_batch(batch_size=1000, max_workers=8, limit=None) -> int:
    """
    [멀티스레드] 방식 배치. 진행률(%) 로깅
    """
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

            batch_processed = 0
            with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as executor:
                # 각 sub_batch를 스레드 풀에 전달
                futures = [executor.submit(process_property_batch, sb) for sb in (
                    rows[i:i+sub_batch_size] for i in range(0, len(rows), sub_batch_size)
                )]

                for future in concurrent.futures.as_completed(futures):
                    try:
                        batch_processed += future.result()
                    except Exception as e:
                        logger.error("[Sub-batch error] %s", e)

            total_processed += batch_processed
            offset += batch_size

            pct = (total_processed / total_count) * 100.0 if total_count else 0
            elapsed = time.time() - start_time
            logger.info("[Progress] %d/%d (%.2f%%) in %.1fs", total_processed, total_count, pct, elapsed)

            if total_processed >= total_count:
                break

    except Exception as e:
        logger.error("[Batch Error] %s", e)
    finally:
        session.close()

    logger.info("[Done] Processed %d/%d", total_processed, (limit or -1))
    return total_processed


def recalculate_incomplete_scores_batch(batch_size=1000, max_workers=8) -> int:
    """
    [멀티스레드] 계산 누락 매물(혹은 모든 카테고리 중 하나라도 count=0인 매물)만 재계산
    """
    initialize_poi_cache()

    session = SessionLocal()
    try:
        query = text("""
            SELECT p.property_id, p.latitude, p.longitude
            FROM property p
            LEFT JOIN property_score ps ON p.property_id = ps.property_id
            WHERE ps.property_id IS NULL
               OR ps.transport_count = 0
               OR ps.restaurant_count = 0
               OR ps.health_count = 0
               OR ps.convenience_count = 0
               OR ps.cafe_count = 0
               OR ps.chicken_count = 0
               OR ps.leisure_count = 0
            ORDER BY p.property_id
        """)
        rows = session.execute(query).fetchall()
    except Exception as e:
        logger.error("[Incomplete Query Error] %s", e)
        session.close()
        return 0

    total = len(rows)
    logger.info("[Incomplete Batch] Found %d incomplete properties", total)

    session.close()  # 조회용 세션 종료

    total_processed = 0
    start_time = time.time()

    # 큰 리스트를 batch_size씩 나누어, 다시 서브배치로 분할
    for offset in range(0, total, batch_size):
        batch_rows = rows[offset:offset+batch_size]
        sub_batch_size = len(batch_rows) // max_workers if max_workers > 0 else len(batch_rows)
        if sub_batch_size < 1:
            sub_batch_size = 1

        batch_processed = 0
        with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as executor:
            futures = [executor.submit(process_property_batch, sb) for sb in (
                batch_rows[i:i+sub_batch_size] for i in range(0, len(batch_rows), sub_batch_size)
            )]
            for future in concurrent.futures.as_completed(futures):
                try:
                    batch_processed += future.result()
                except Exception as e:
                    logger.error("[Incomplete Sub-batch error] %s", e)

        total_processed += batch_processed

        pct = (total_processed / total) * 100.0 if total else 0
        elapsed = time.time() - start_time
        logger.info("[Incomplete Progress] %d/%d (%.2f%%) in %.1fs", total_processed, total, pct, elapsed)

    logger.info("[Incomplete Done] Recalculated scores for %d/%d incomplete properties.", total_processed, total)
    return total_processed
