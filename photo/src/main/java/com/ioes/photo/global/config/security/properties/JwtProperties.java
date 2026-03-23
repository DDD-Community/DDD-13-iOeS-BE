package com.ioes.photo.global.config.security.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정 프로퍼티.
 *
 *
 * @param secret       HMAC-SHA256 서명에 사용하는 Base64 인코딩 비밀 키
 * @param expirationMs 토큰 유효 시간 (밀리초)
 * @author 황제연
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMs) {
}