package com.ioes.photo.global.config.security.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 토큰 관련 설정 프로퍼티.
 *
 * @param refreshExpirationMs Refresh Token 유효 시간
 * @param refreshKeyPrefix Redis Refresh Token 키 prefix
 * @param userTokensKeyPrefix Redis 유저별 토큰 Set 키 prefix
 * @param providerRtKeyPrefix Redis Provider Refresh Token 키 prefix
 * @author 황제연
 */
@ConfigurationProperties(prefix = "app.token")
public record TokenProperties(
    long refreshExpirationMs,
    String refreshKeyPrefix,
    String userTokensKeyPrefix,
    String providerRtKeyPrefix
    long restoreTokenExpirationMs
) {
}