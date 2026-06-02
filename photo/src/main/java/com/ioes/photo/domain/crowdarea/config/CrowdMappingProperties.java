package com.ioes.photo.domain.crowdarea.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 스팟 → 혼잡도 장소 자동 매핑 설정.
 *
 * @param maxDistanceMeters 최근접 장소까지의 거리가 이 값을 초과하면 매핑하지 않는다(미터).
 *                          오매핑 방지를 위한 임계값으로, 운영 중 PM 협의로 조정될 수 있다.
 * @author 김성민
 */
@ConfigurationProperties(prefix = "app.crowd.mapping")
public record CrowdMappingProperties(
    double maxDistanceMeters
) {
}
