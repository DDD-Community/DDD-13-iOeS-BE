-- 스팟 이미지 출처 구분(INTERNAL=자사 스토리지, EXTERNAL=외부 호스팅 URL hotlink) 컬럼 추가.
-- PostgreSQL 11+ 에서는 ADD COLUMN에 DEFAULT를 지정하면 테이블 재작성 없이 기존 행에도 즉시 채워진다(V16과 동일 원리).
-- 기존 행은 전부 자사 스토리지에 업로드된 이미지이므로 DEFAULT 'I'(INTERNAL)로 백필되며, 별도 UPDATE는 필요 없다.
-- IF NOT EXISTS 사용으로 신규/기존 환경 모두 안전하게 실행됨
ALTER TABLE spot_images
    ADD COLUMN IF NOT EXISTS image_source_type VARCHAR(1) NOT NULL DEFAULT 'I';
