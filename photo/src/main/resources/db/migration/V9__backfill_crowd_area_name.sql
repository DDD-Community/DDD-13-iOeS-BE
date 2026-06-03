-- ============================================================
-- V9: 기존 스팟 혼잡도 장소명(crowd_area_name) backfill
-- crowd_area_name 이 비어 있던 스팟은 CrowdCollector 대상에서 제외되어 혼잡도가 수집되지 않았다.
-- V8 로 적재된 장소(crowd_areas) 중 가장 가까운 장소를 PostGIS 로 찾아 채운다.
-- 거리는 ST_DistanceSphere(구면 거리 ≈ 런타임 CrowdAreaMapper 의 Haversine)로 계산하며,
-- 장소가 약 121개로 적어 전체를 정렬해 최근접을 고른다(런타임과 동일하게 정확한 최근접).
-- 임계값은 런타임 자동 매핑(app.crowd.mapping.max-distance-meters)의 작성 시점 기본값(3km)으로 고정한다.
-- ============================================================

UPDATE spots s
SET crowd_area_name = nearest.area_name
FROM LATERAL (
    SELECT ca.area_name,
           ST_DistanceSphere(s.location,
               ST_SetSRID(ST_MakePoint(ca.longitude, ca.latitude), 4326)) AS distance_meters
    FROM crowd_areas ca
    ORDER BY distance_meters
    LIMIT 1
) nearest
WHERE s.status = 'B'
  AND s.crowd_area_name IS NULL
  AND s.location IS NOT NULL
  AND nearest.distance_meters <= 3000;
