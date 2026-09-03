-- ============================================================
-- V19: spots 테이블에 spot_regions 매칭용 region_id 컬럼 추가
-- 값 백필은 V20 에서 처리한다.
-- FK는 컨벤션상 사용하지 않는다(다른 참조 컬럼과 동일하게 애플리케이션 레벨에서 정합성을 보장).
-- IF NOT EXISTS 사용으로 신규/기존 환경 모두 안전하게 실행됨
-- ============================================================

ALTER TABLE spots ADD COLUMN IF NOT EXISTS region_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_spots_region_id ON spots (region_id);
