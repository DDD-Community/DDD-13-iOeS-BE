-- ============================================================
-- V22: 스팟 검수완료 알림 히스토리 테이블 신설
-- 검수(승인/반려) 완료 시점에 이벤트 기반으로 자동 적재되는 사용자별 알림 이력이다.
-- spot_open_requests 컨벤션과 동일하게 FK 제약 없이 spot_id/user_id(PK)만 저장한다.
-- IF NOT EXISTS 사용으로 신규/기존 환경 모두 안전하게 실행됨
-- ============================================================

CREATE TABLE IF NOT EXISTS spot_open_review_history (
    id             BIGSERIAL PRIMARY KEY,
    spot_id        BIGINT      NOT NULL,
    user_id        BIGINT      NOT NULL,
    spot_status    VARCHAR(4)  NOT NULL,
    reject_reason  VARCHAR(4),
    reject_detail  TEXT,
    check_yn       VARCHAR(1)  NOT NULL DEFAULT 'N',
    created_at     TIMESTAMP   NOT NULL,
    updated_at     TIMESTAMP   NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_spot_open_review_history_user_check
    ON spot_open_review_history (user_id, check_yn);
