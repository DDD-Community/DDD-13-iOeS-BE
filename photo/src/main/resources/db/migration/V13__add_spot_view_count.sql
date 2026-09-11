-- ============================================================
-- V13: 스팟 조회수(view_count) 컬럼 추가
-- IF NOT EXISTS 사용으로 신규/기존 환경 모두 안전하게 실행됨
-- 기본값 0 (bookmark_count와 동일한 비정규화 카운터)
-- ============================================================

ALTER TABLE spots ADD COLUMN IF NOT EXISTS view_count BIGINT NOT NULL DEFAULT 0;
