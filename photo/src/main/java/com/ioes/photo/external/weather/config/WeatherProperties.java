package com.ioes.photo.external.weather.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 기상청 단기예보 수집 관련 설정.
 *
 * @param availabilityDelayMinutes 발표 시각으로부터 조회 가능해지기까지의 지연(분).
 *                                 기상청 측 처리 지연 실측 보정값으로, 운영 중 조정될 수 있다.
 * @author 김성민
 */
@ConfigurationProperties(prefix = "app.weather")
public record WeatherProperties(
    int availabilityDelayMinutes
) {
}
