-- ============================================================
-- V12: 스팟 출처 구분(source) 컬럼 추가
-- IF NOT EXISTS 사용으로 신규/기존 환경 모두 안전하게 실행됨
-- 기본값 'C' = CURATION (기존 행은 전량 큐레이션으로 백필)
-- ============================================================

ALTER TABLE spots ADD COLUMN IF NOT EXISTS source VARCHAR(4) NOT NULL DEFAULT 'C';
