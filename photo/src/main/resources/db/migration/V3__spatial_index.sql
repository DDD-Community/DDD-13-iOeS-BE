-- ============================================================
-- V3: PostGIS GIST 인덱스
-- 기존 db/spatial-index.sql 내용을 Flyway 관리로 편입
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_spots_location ON spots USING GIST (location);
