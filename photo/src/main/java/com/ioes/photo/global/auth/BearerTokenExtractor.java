package com.ioes.photo.global.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

/**
 * HTTP 요청에서 Bearer 토큰을 추출하는 유틸리티.
 *
 * @author 황제연
 */
public final class BearerTokenExtractor {

    private static final String BEARER_PREFIX = "Bearer ";

    private BearerTokenExtractor() {}

    /**
     * Authorization 헤더에서 Bearer 토큰을 추출한다.
     * 헤더가 없거나 Bearer 형식이 아니면 null을 반환한다.
     */
    public static String extract(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
