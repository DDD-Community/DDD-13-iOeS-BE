-- ============================================================
-- V1: Baseline Schema (DEV 기준 현재 상태 전체)
-- IF NOT EXISTS 사용: 신규 환경(테이블 없음)과 기존 환경(테이블 있음) 모두 안전하게 실행됨
-- baseline-version=0 설정으로 이 파일은 항상 실행됨 (baseline-on-migrate 트리거 시도 시 V0 기준)
-- ============================================================

CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS users
(
    id                BIGSERIAL PRIMARY KEY,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,
    provider          VARCHAR(4)   NOT NULL,
    provider_user_id  VARCHAR      NOT NULL,
    email             VARCHAR,
    nickname          VARCHAR,
    hash_tag          BIGINT,
    profile_image_url VARCHAR,
    profile_image_key VARCHAR,
    archive_image_key VARCHAR,
    archive_name      VARCHAR(20)  NOT NULL DEFAULT '나의 보관함',
    deleted_at        TIMESTAMP
);

DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_users_provider_provider_user_id') THEN
            ALTER TABLE users ADD CONSTRAINT uk_users_provider_provider_user_id UNIQUE (provider, provider_user_id);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_users_nickname_hash_tag') THEN
            ALTER TABLE users ADD CONSTRAINT uk_users_nickname_hash_tag UNIQUE (nickname, hash_tag);
        END IF;
    END
$$;

CREATE TABLE IF NOT EXISTS spots
(
    id              BIGSERIAL PRIMARY KEY,
    created_at      TIMESTAMP         NOT NULL,
    updated_at      TIMESTAMP         NOT NULL,
    name            VARCHAR(100)      NOT NULL,
    comment         TEXT,
    theme           VARCHAR(4)        NOT NULL,
    latitude        DOUBLE PRECISION  NOT NULL,
    longitude       DOUBLE PRECISION  NOT NULL,
    location        GEOMETRY(Point, 4326),
    address         VARCHAR(255),
    status          VARCHAR(4)        NOT NULL,
    grid_nx         INTEGER,
    grid_ny         INTEGER,
    crowd_area_name VARCHAR(50),
    user_id         BIGINT,
    bookmark_count  BIGINT            NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_spots_theme ON spots (theme);
CREATE INDEX IF NOT EXISTS idx_spots_status ON spots (status);
CREATE INDEX IF NOT EXISTS idx_spots_crowd_area_name ON spots (crowd_area_name);

CREATE TABLE IF NOT EXISTS spot_images
(
    spot_id           BIGINT PRIMARY KEY,
    image_key         VARCHAR   NOT NULL,
    thumbnail_key     VARCHAR,
    original_filename VARCHAR,
    content_type      VARCHAR,
    recorded_date     DATE,
    recorded_time     TIME,
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS spot_reports
(
    id         BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    spot_id    BIGINT       NOT NULL,
    user_id    BIGINT       NOT NULL,
    status     VARCHAR(1)   NOT NULL,
    content    VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS spot_info
(
    spot_id                   BIGINT PRIMARY KEY,
    congestion_level          VARCHAR(4),
    congestion_message        TEXT,
    population_min            INTEGER,
    population_max            INTEGER,
    congestion_updated_at     TIMESTAMP,
    weather_sky               VARCHAR(4),
    weather_precipitation     VARCHAR(4),
    precipitation_probability INTEGER,
    temperature               DOUBLE PRECISION,
    weather_updated_at        TIMESTAMP,
    astronomy_date            DATE,
    sunrise_time              TIME,
    sunset_time               TIME,
    created_at                TIMESTAMP NOT NULL,
    updated_at                TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS saved_spot_archives
(
    id         BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    user_id    BIGINT    NOT NULL,
    spot_id    BIGINT    NOT NULL,
    deleted_at TIMESTAMP
);

DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_saved_spot_archives_user_spot') THEN
            ALTER TABLE saved_spot_archives ADD CONSTRAINT uk_saved_spot_archives_user_spot UNIQUE (user_id, spot_id);
        END IF;
    END
$$;

CREATE INDEX IF NOT EXISTS idx_saved_spot_archives_user_id ON saved_spot_archives (user_id);

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

CREATE TABLE IF NOT EXISTS withdrawal_reasons
(
    id          BIGSERIAL PRIMARY KEY,
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP   NOT NULL,
    user_id     BIGINT      NOT NULL,
    reason_type VARCHAR(10) NOT NULL,
    content     TEXT
);
