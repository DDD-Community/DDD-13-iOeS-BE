-- ============================================================
-- V21: spots 테이블에 노출(release) on/off 플래그(rel_yn) 추가
-- 검수 flow(status)와 독립적인 별도 플래그로, 기본값은 'N'이다.
-- 이미 공개(PUBLISHED='B') 상태인 기존 스팟은 배포 직후 조회에서 사라지지 않도록 'Y'로 백필한다.
-- IF NOT EXISTS 사용으로 신규/기존 환경 모두 안전하게 실행됨
-- ============================================================

ALTER TABLE spots ADD COLUMN IF NOT EXISTS rel_yn VARCHAR(1) NOT NULL DEFAULT 'N';

UPDATE spots SET rel_yn = 'Y' WHERE status = 'B' AND rel_yn = 'N';

CREATE INDEX IF NOT EXISTS idx_spots_rel_yn ON spots (rel_yn);
