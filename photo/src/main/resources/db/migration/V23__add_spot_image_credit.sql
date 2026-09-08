-- 스팟 이미지 출처 표기(예: "ⓒ 한국관광공사", "사진 제공: 홍길동") 컬럼 추가.
-- 관리자 큐레이션 스팟(spots.user_id IS NULL)에만 값이 채워지며, 유저 등록 스팟은 항상 NULL이다.
-- IF NOT EXISTS 사용으로 신규/기존 환경 모두 안전하게 실행됨
ALTER TABLE spot_images
    ADD COLUMN IF NOT EXISTS credit VARCHAR(200);
