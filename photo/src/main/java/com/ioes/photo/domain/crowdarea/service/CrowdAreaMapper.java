package com.ioes.photo.domain.crowdarea.service;

import com.ioes.photo.domain.crowdarea.config.CrowdMappingProperties;
import com.ioes.photo.domain.crowdarea.entity.CrowdArea;
import com.ioes.photo.domain.crowdarea.repository.CrowdAreaRepository;
import com.ioes.photo.global.common.util.GeoUtils;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 스팟 좌표를 가장 가까운 서울시 혼잡도 장소명으로 매핑한다.
 *
 * 장소 목록(약 121개)은 거의 고정이라 최초 1회 메모리에 적재해 재사용한다.
 * 최근접 장소까지의 거리가 임계값을 초과하면 오매핑 방지를 위해 매핑하지 않는다.
 *
 * @author 김성민
 */
@Component
@RequiredArgsConstructor
public class CrowdAreaMapper {

    private final CrowdAreaRepository crowdAreaRepository;
    private final CrowdMappingProperties properties;

    private volatile List<CrowdArea> cache;

    /**
     * 좌표에서 가장 가까운 혼잡도 장소명을 찾는다.
     *
     * @return 임계값 이내의 장소명, 없으면 empty
     */
    public Optional<String> findNearestAreaName(double latitude, double longitude) {
        CrowdArea nearest = null;
        double nearestMeters = Double.MAX_VALUE;
        for (CrowdArea area : areas()) {
            double meters = GeoUtils.distanceMeters(latitude, longitude, area.getLatitude(), area.getLongitude());
            if (meters < nearestMeters) {
                nearestMeters = meters;
                nearest = area;
            }
        }
        if (nearest == null || nearestMeters > properties.maxDistanceMeters()) {
            return Optional.empty();
        }
        return Optional.of(nearest.getAreaName());
    }

    private List<CrowdArea> areas() {
        List<CrowdArea> local = cache;
        if (local == null) {
            synchronized (this) {
                local = cache;
                if (local == null) {
                    List<CrowdArea> loaded = List.copyOf(crowdAreaRepository.findAll());
                    if (loaded.isEmpty()) {
                        return loaded;
                    }
                    local = loaded;
                    cache = local;
                }
            }
        }
        return local;
    }
}
