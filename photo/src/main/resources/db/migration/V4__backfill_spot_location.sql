-- ============================================================
-- V4: 기존 스팟 location 컬럼 backfill
-- V2에서 location 컬럼 추가 시 기존 데이터 미반영 → NULL 상태
-- latitude/longitude 값으로 geometry 재구성
-- ============================================================

UPDATE spots
SET location = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)
WHERE location IS NULL
  AND latitude IS NOT NULL
  AND longitude IS NOT NULL;
