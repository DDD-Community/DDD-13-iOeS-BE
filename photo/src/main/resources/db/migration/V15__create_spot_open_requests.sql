-- ============================================================
-- V15: 오픈 신청 이력 테이블
--   사용자가 '오픈하기'를 누를 때마다 1행이 적재되고, 검수(승인/반려) 또는 철회 시 마감된다.
--   spot_reviews 는 운영자의 처리 결과만 남기므로, 사용자의 신청 행위 자체는 여기서 추적한다.
--   상태값(SpotOpenRequestStatus)은 애플리케이션 코드에서 관리하므로 CHECK 제약을 두지 않는다.
--     Q=신청(진행중), A=승인마감, R=반려마감, C=철회
--   IF NOT EXISTS 사용으로 신규/기존 환경 모두 안전하게 실행됨
-- ============================================================

CREATE TABLE IF NOT EXISTS spot_open_requests (
    id             BIGSERIAL PRIMARY KEY,
    spot_id        BIGINT      NOT NULL,
    user_id        BIGINT      NOT NULL,
    status         VARCHAR(4)  NOT NULL,
    requested_at   TIMESTAMP   NOT NULL,
    resolved_at    TIMESTAMP,
    spot_review_id BIGINT,
    created_at     TIMESTAMP   NOT NULL,
    updated_at     TIMESTAMP   NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_spot_open_requests_spot_id ON spot_open_requests (spot_id);
CREATE INDEX IF NOT EXISTS idx_spot_open_requests_user_id ON spot_open_requests (user_id);

-- 스팟당 진행 중(Q) 신청은 최대 1건.
-- 부분 유니크 인덱스는 Hibernate DDL 생성 대상이 아니므로, 이 마이그레이션을 통해서만 만들어진다.
-- 애플리케이션 단에서는 상태 전이 시 비관적 락으로 1차 방어하고, 이 인덱스가 최종 안전망이 된다.
CREATE UNIQUE INDEX IF NOT EXISTS uk_spot_open_requests_in_flight
    ON spot_open_requests (spot_id) WHERE status = 'Q';

-- 검수 기능 도입(V14) 이전에 이미 신청/처리된 스팟의 이력을 보정한다.
-- 관리자 큐레이션(user_id IS NULL)과 아직 신청한 적 없는 DRAFT('D')는 대상이 아니다.
INSERT INTO spot_open_requests
    (spot_id, user_id, status, requested_at, resolved_at, created_at, updated_at)
SELECT s.id,
       s.user_id,
       CASE s.status
           WHEN 'P' THEN 'Q'
           WHEN 'V' THEN 'Q'
           WHEN 'B' THEN 'A'
           ELSE 'R'
       END,
       COALESCE(s.applied_at, s.created_at),
       CASE WHEN s.status IN ('B', 'R') THEN COALESCE(s.reviewed_at, s.updated_at) END,
       NOW(),
       NOW()
FROM spots s
WHERE s.user_id IS NOT NULL
  AND s.status IN ('P', 'V', 'B', 'R')
  AND s.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM spot_open_requests r WHERE r.spot_id = s.id);
