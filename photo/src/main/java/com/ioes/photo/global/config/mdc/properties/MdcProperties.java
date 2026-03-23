package com.ioes.photo.global.config.mdc.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MDC 필터설정 프로퍼티 레코드
 *
 *
 * @param forwardedIpHeader 실제 클라이언트 IP를 담는 HTTP 요청 헤더명 (예: X-Forwarded-For)
 * @param keys              MDC에 저장할 컨텍스트 키 이름 모음
 * @author 황제연
 */
@ConfigurationProperties(prefix = "app.mdc")
public record MdcProperties(
    String forwardedIpHeader,
    Keys keys
) {
    /**
     * MDC 컨텍스트 키 이름 모음.
     *
     * @param requestId  요청 고유 ID 키 이름
     * @param userId     인증된 사용자 ID 키 이름
     * @param requestUri 요청 URI 키 이름
     * @param method     HTTP 메서드 키 이름
     * @param clientIp   클라이언트 IP 키 이름
     */
    public record Keys(
        String requestId,
        String userId,
        String requestUri,
        String method,
        String clientIp
    ) {}
}
