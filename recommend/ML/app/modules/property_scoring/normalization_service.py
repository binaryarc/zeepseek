from sqlalchemy import text
from app.config.database import SessionLocal
from app.modules.property_scoring.dynamic_score_min_max import get_score_min_max_values
from app.utils.normalization_utils import scale_score  # 기존 min-max 스케일 함수
from app.utils.logger import logger
import pandas as pd
import numpy as np
from scipy.stats import yeojohnson

def normalize_scores_and_update():
    """
    기존 min–max 정규화를 적용하여 0~100 점으로 업데이트 (변경 없음).
    """
    min_max_dict = get_score_min_max_values()
    session = SessionLocal()

    try:
        rows = session.execute(text("""
            SELECT property_id,
                   transport_score, restaurant_score, health_score, convenience_score,
                   cafe_score, chicken_score, leisure_score
            FROM property_score
        """)).fetchall()

        for row in rows:
            pid = row._mapping["property_id"]
            update_data = {}

            for category in [
                "transport_score", "restaurant_score", "health_score",
                "convenience_score", "cafe_score", "chicken_score", "leisure_score"
            ]:
                original = row._mapping[category]
                min_val, max_val = min_max_dict.get(category, (0, 1))
                scaled = scale_score(original, min_val, max_val)
                update_data[category] = scaled

            stmt = text("""
                UPDATE property_score
                SET transport_score = :transport_score,
                    restaurant_score = :restaurant_score,
                    health_score = :health_score,
                    convenience_score = :convenience_score,
                    cafe_score = :cafe_score,
                    chicken_score = :chicken_score,
                    leisure_score = :leisure_score
                WHERE property_id = :property_id
            """)
            session.execute(stmt, {**update_data, "property_id": pid})

        session.commit()
        logger.info("[Normalization] Successfully updated scores with min-max normalization (0-100 scale).")
    except Exception as e:
        session.rollback()
        logger.error("[Normalization Error] %s", e)
    finally:
        session.close()


def normalize_scores_yeojohnson_and_update():
    """
    property_score 테이블의 0~100 범위로 저장된 점수를
    Yeo–Johnson 변환을 적용한 후, 다시 0~100 범위로 리스케일하여 DB에 업데이트합니다.
    """
    session = SessionLocal()
    
    try:
        # 기존 데이터 로드 (이미 0~100 정규화된 값)
        df = pd.read_sql(text("""
            SELECT property_id,
                   transport_score, restaurant_score, health_score, convenience_score,
                   cafe_score, chicken_score, leisure_score
            FROM property_score
        """), session.bind)
        
        if df.empty:
            logger.info("No property score data found for Yeo-Johnson normalization.")
            return

        # 각 카테고리별 Yeo–Johnson 변환 및 0~100 재스케일링
        df_yeo = df.copy()
        for col in [
            "transport_score", "restaurant_score", "health_score", "convenience_score",
            "cafe_score", "chicken_score", "leisure_score"
        ]:
            # Yeo–Johnson 변환 (값이 음수일 경우에도 처리 가능)
            transformed, lmbda = yeojohnson(df[col].values)
            # 리스케일: 선형 스케일 (최소값 0, 최대값 100)
            t_min, t_max = transformed.min(), transformed.max()
            transformed_scaled = 100 * ((transformed - t_min) / (t_max - t_min))
            df_yeo[col] = transformed_scaled
            logger.info(f"[Yeo-Johnson] {col}: lambda={lmbda:.2f}, original range=({df[col].min():.2f}, {df[col].max():.2f}), transformed range=({transformed_scaled.min():.2f}, {transformed_scaled.max():.2f})")
        
        # DB 업데이트
        for idx, row in df_yeo.iterrows():
            stmt = text("""
                UPDATE property_score
                SET transport_score = :transport_score,
                    restaurant_score = :restaurant_score,
                    health_score = :health_score,
                    convenience_score = :convenience_score,
                    cafe_score = :cafe_score,
                    chicken_score = :chicken_score,
                    leisure_score = :leisure_score
                WHERE property_id = :property_id
            """)
            params = {
                "transport_score": row["transport_score"],
                "restaurant_score": row["restaurant_score"],
                "health_score": row["health_score"],
                "convenience_score": row["convenience_score"],
                "cafe_score": row["cafe_score"],
                "chicken_score": row["chicken_score"],
                "leisure_score": row["leisure_score"],
                "property_id": row["property_id"]
            }
            session.execute(stmt, params)
        
        session.commit()
        logger.info("[Yeo-Johnson Normalization] Successfully updated property scores with Yeo-Johnson transformation (0-100 scale).")
    except Exception as e:
        session.rollback()
        logger.error("[Yeo-Johnson Normalization Error] %s", e)
    finally:
        session.close()
