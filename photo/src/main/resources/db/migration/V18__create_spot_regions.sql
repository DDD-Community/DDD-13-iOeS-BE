-- ============================================================
-- V18: 지역 필터용 spot_regions 테이블 신설 + 서울/대전 시드
-- 이후 지역 추가, 조회 API 성능, 어드민 페이지 활용을 위해 enum 대신 테이블로 관리한다.
-- IF NOT EXISTS 사용으로 신규/기존 환경 모두 안전하게 실행됨
-- ============================================================

CREATE TABLE IF NOT EXISTS spot_regions
(
    region_id   BIGSERIAL PRIMARY KEY,
    region_name VARCHAR(50) NOT NULL,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    new_date    TIMESTAMP   NOT NULL DEFAULT now(),
    edt_date    TIMESTAMP   NOT NULL DEFAULT now()
);

DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_spot_regions_region_name') THEN
            ALTER TABLE spot_regions ADD CONSTRAINT uk_spot_regions_region_name UNIQUE (region_name);
        END IF;
    END
$$;

-- region_id 는 spots.address 를 LIKE 매칭할 때 그대로 상수처럼 참조되므로 (1=서울, 2=대전) 값을 고정한다.
INSERT INTO spot_regions (region_id, region_name, is_active, new_date, edt_date)
VALUES (1, '서울', TRUE, now(), now()),
       (2, '대전', TRUE, now(), now())
ON CONFLICT (region_id) DO NOTHING;

-- 위에서 명시적으로 채운 id 이후부터 시퀀스가 이어지도록 동기화한다(이후 어드민 등록은 자동 채번).
SELECT setval(pg_get_serial_sequence('spot_regions', 'region_id'),
              (SELECT MAX(region_id) FROM spot_regions));
