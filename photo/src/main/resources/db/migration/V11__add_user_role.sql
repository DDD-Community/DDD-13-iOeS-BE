-- ============================================================
-- V11: 사용자 권한(role) 컬럼 추가
-- IF NOT EXISTS 사용으로 신규/기존 환경 모두 안전하게 실행됨
-- 기본값 'C' = USER_CUSTOMER (기존 행 보호)
-- ============================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(4) NOT NULL DEFAULT 'C';
