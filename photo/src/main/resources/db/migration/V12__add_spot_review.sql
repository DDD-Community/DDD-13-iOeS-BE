-- ============================================================
-- V12: 스팟 검수(어드민) 지원 - 오픈 신청/검수 처리 정보 + 검수 이력 테이블
-- IF NOT EXISTS 사용으로 신규/기존 환경 모두 안전하게 실행됨
-- 상태값(SpotStatus)은 애플리케이션 코드에서 관리하므로 별도 제약 변경 없음
--   추가 상태: DRAFT('D', 나만보기), RE_REVIEW_PENDING('V', 재검토대기)
-- ============================================================

-- 오픈 신청 일시(검수 큐 정렬 기준) / 검수 처리 일시 / 처리자
ALTER TABLE spots ADD COLUMN IF NOT EXISTS applied_at  TIMESTAMP;
ALTER TABLE spots ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;
ALTER TABLE spots ADD COLUMN IF NOT EXISTS reviewer_id BIGINT;

-- 검수 이력 (append-only) : 승인/반려 처리 시마다 1행 적재
CREATE TABLE IF NOT EXISTS spot_reviews (
    id          BIGSERIAL PRIMARY KEY,
    spot_id     BIGINT      NOT NULL,
    decision    VARCHAR(4)  NOT NULL,
    reason      VARCHAR(4),
    detail      TEXT,
    reviewer_id BIGINT      NOT NULL,
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP   NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_spot_reviews_spot_id ON spot_reviews (spot_id);
