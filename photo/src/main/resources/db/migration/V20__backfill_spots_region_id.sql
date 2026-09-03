-- ============================================================
-- V20: 기존 spots.region_id 백필
-- address 접두어 기준으로 매칭한다: '서울%' -> 1(서울), '대전%' -> 2(대전).
-- 그 외 주소(또는 주소 없음)는 대상 지역이 아직 없으므로 NULL로 남긴다.
-- ============================================================

UPDATE spots SET region_id = 1 WHERE region_id IS NULL AND address LIKE '서울%';
UPDATE spots SET region_id = 2 WHERE region_id IS NULL AND address LIKE '대전%';
