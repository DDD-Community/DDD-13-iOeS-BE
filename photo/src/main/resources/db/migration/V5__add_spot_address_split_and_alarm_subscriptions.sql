-- ============================================================
-- V5: 나만의 스팟 주소 분리(도로명/지번) + 촬영조건 알림 구독
-- IF NOT EXISTS 사용으로 신규/기존 환경 모두 안전하게 실행됨
-- ============================================================

ALTER TABLE spots ADD COLUMN IF NOT EXISTS address_road  VARCHAR(255);
ALTER TABLE spots ADD COLUMN IF NOT EXISTS address_jibun VARCHAR(255);

CREATE TABLE IF NOT EXISTS spot_alarm_subscriptions
(
    id         BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    user_id    BIGINT    NOT NULL,
    spot_id    BIGINT    NOT NULL,
    enabled    BOOLEAN   NOT NULL DEFAULT TRUE
);

DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_spot_alarm_subscriptions_user_spot') THEN
            ALTER TABLE spot_alarm_subscriptions ADD CONSTRAINT uk_spot_alarm_subscriptions_user_spot UNIQUE (user_id, spot_id);
        END IF;
    END
$$;

CREATE INDEX IF NOT EXISTS idx_spot_alarm_subscriptions_user_id ON spot_alarm_subscriptions (user_id);
