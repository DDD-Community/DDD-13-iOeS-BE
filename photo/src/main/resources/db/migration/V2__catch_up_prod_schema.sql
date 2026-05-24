-- ============================================================
-- V2: 운영 DB 스키마 Catch-up
-- DEV 대비 운영(test/prod)에 누락된 모든 테이블/컬럼을 안전하게 적용
-- IF NOT EXISTS 구문으로 멱등성 보장 (DEV에서 실행해도 충돌 없음)
-- ============================================================

CREATE EXTENSION IF NOT EXISTS postgis;

-- ── users ────────────────────────────────────────────────────

ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_image_key VARCHAR;
ALTER TABLE users ADD COLUMN IF NOT EXISTS archive_image_key VARCHAR;
ALTER TABLE users ADD COLUMN IF NOT EXISTS archive_name VARCHAR(20);

UPDATE users SET archive_name = '나의 보관함' WHERE archive_name IS NULL;

ALTER TABLE users ALTER COLUMN archive_name SET NOT NULL;
ALTER TABLE users ALTER COLUMN archive_name SET DEFAULT '나의 보관함';

-- ── spots ────────────────────────────────────────────────────

ALTER TABLE spots ADD COLUMN IF NOT EXISTS bookmark_count BIGINT;

UPDATE spots SET bookmark_count = 0 WHERE bookmark_count IS NULL;

ALTER TABLE spots ALTER COLUMN bookmark_count SET NOT NULL;
ALTER TABLE spots ALTER COLUMN bookmark_count SET DEFAULT 0;

ALTER TABLE spots ADD COLUMN IF NOT EXISTS location GEOMETRY(Point, 4326);
ALTER TABLE spots ADD COLUMN IF NOT EXISTS crowd_area_name VARCHAR(50);

-- ── spot_images ──────────────────────────────────────────────

ALTER TABLE spot_images ADD COLUMN IF NOT EXISTS recorded_time TIME;

-- ── spot_info ────────────────────────────────────────────────

ALTER TABLE spot_info ADD COLUMN IF NOT EXISTS weather_precipitation VARCHAR(4);
ALTER TABLE spot_info ADD COLUMN IF NOT EXISTS precipitation_probability INTEGER;

-- ── saved_spot_archives (신설) ───────────────────────────────

CREATE TABLE IF NOT EXISTS saved_spot_archives
(
    id         BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    user_id    BIGINT    NOT NULL,
    spot_id    BIGINT    NOT NULL,
    deleted_at TIMESTAMP,
    CONSTRAINT uk_saved_spot_archives_user_spot UNIQUE (user_id, spot_id)
);

DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1
                       FROM pg_indexes
                       WHERE tablename = 'saved_spot_archives'
                         AND indexname = 'idx_saved_spot_archives_user_id') THEN
            CREATE INDEX idx_saved_spot_archives_user_id ON saved_spot_archives (user_id);
        END IF;
    END
$$;

-- ── bbs_posts (신설) ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS bbs_posts
(
    id         BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    master_id  BIGINT       NOT NULL,
    title      VARCHAR(255) NOT NULL,
    content    TEXT         NOT NULL,
    pinned     BOOLEAN      NOT NULL DEFAULT FALSE
);

-- ── withdrawal_reasons (신설) ─────────────────────────────────

CREATE TABLE IF NOT EXISTS withdrawal_reasons
(
    id          BIGSERIAL PRIMARY KEY,
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP   NOT NULL,
    user_id     BIGINT      NOT NULL,
    reason_type VARCHAR(10) NOT NULL,
    content     TEXT
);

-- ── 인덱스 보정 (누락된 경우 추가) ──────────────────────────────

DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE tablename = 'spots' AND indexname = 'idx_spots_theme') THEN
            CREATE INDEX idx_spots_theme ON spots (theme);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE tablename = 'spots' AND indexname = 'idx_spots_status') THEN
            CREATE INDEX idx_spots_status ON spots (status);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE tablename = 'spots' AND indexname = 'idx_spots_crowd_area_name') THEN
            CREATE INDEX idx_spots_crowd_area_name ON spots (crowd_area_name);
        END IF;
    END
$$;
