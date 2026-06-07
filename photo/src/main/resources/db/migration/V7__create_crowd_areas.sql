-- ============================================================
-- V7: 서울시 혼잡도 장소(crowd_areas) 테이블
-- 스팟 좌표를 가장 가까운 서울시 실시간 도시데이터 장소에 매핑하기 위한 참조 데이터.
-- 시드는 V8에서 적재한다.
-- ============================================================

CREATE TABLE IF NOT EXISTS crowd_areas
(
    area_code VARCHAR(20)      PRIMARY KEY,
    area_name VARCHAR(50)      NOT NULL,
    category  VARCHAR(30),
    latitude  DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_crowd_areas_area_name ON crowd_areas (area_name);
