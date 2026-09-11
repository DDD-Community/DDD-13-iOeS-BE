-- ============================================================
-- V16: 스팟 좋아요(추천)
--   북마크(saved_spot_archives)와는 별개 도메인이다. 북마크는 '나중에 보려고 저장',
--   좋아요는 '추천'으로 의미가 다르며 목록의 추천순 정렬 기준이 된다.
--   카운터는 spots.like_count 로 비정규화하고 atomic UPDATE 로만 갱신한다.
--   기존 bookmark_count 및 그 증감 로직은 변경하지 않는다.
--   IF NOT EXISTS 사용으로 신규/기존 환경 모두 안전하게 실행됨
-- ============================================================

-- PostgreSQL 11+ 에서는 DEFAULT 가 있어도 테이블 재작성 없이 즉시 추가된다.
ALTER TABLE spots ADD COLUMN IF NOT EXISTS like_count BIGINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS spot_likes (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    spot_id    BIGINT    NOT NULL,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- 같은 사용자가 같은 스팟에 두 번 좋아요할 수 없다.
-- 좋아요 취소를 논리삭제로 처리하므로, 취소 후 재좋아요는 기존 행을 되살려 이 제약과 공존한다.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_spot_likes_user_spot') THEN
        ALTER TABLE spot_likes ADD CONSTRAINT uk_spot_likes_user_spot UNIQUE (user_id, spot_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_spot_likes_user_id ON spot_likes (user_id);
CREATE INDEX IF NOT EXISTS idx_spot_likes_spot_id ON spot_likes (spot_id);
