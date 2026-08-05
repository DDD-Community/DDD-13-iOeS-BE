package com.ioes.photo.global.config.web.properties;

import java.util.Collections;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS 허용 설정 프로퍼티 레코드.
 *
 * @param allowedOrigins 교차 출처 요청을 허용할 Origin 목록(어드민 웹 도메인)
 * @param allowedMethods 허용할 HTTP 메서드 목록
 * @param maxAge         preflight 응답 캐시 시간(초)
 * @author 황제연
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
    List<String> allowedOrigins,
    List<String> allowedMethods,
    Long maxAge
) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? Collections.emptyList() : allowedOrigins;
        allowedMethods = allowedMethods == null ? Collections.emptyList() : allowedMethods;
        maxAge = maxAge == null ? 3600L : maxAge;
    }
}
