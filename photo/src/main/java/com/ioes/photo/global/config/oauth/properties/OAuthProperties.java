package com.ioes.photo.global.config.oauth.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth 공급자별 설정 프로퍼티.
 *
 * @param apple Apple Sign In 설정
 * @param kakao Kakao 로그인 설정
 * @author 황제연
 */
@ConfigurationProperties(prefix = "app.oauth")
public record OAuthProperties(Apple apple, Kakao kakao) {

    /**
     * Apple Sign In 설정.
     *
     * @param clientId Apple Bundle ID (identityToken audience 검증에 사용)
     * @param teamId Apple Developer Team ID (client_secret JWT 생성에 사용)
     * @param keyId Apple Key ID (client_secret JWT 생성에 사용)
     * @param privateKey PKCS8 PEM 형식의 EC 비밀 키 (client_secret JWT 서명에 사용)
     * @param clientSecretTtlMs client_secret JWT 유효 기간
     */
    public record Apple(
        String clientId,
        String teamId,
        String keyId,
        String privateKey,
        long   clientSecretTtlMs
    ) {}

    /**
     * Kakao 로그인 설정.
     *
     * @param clientId Kakao REST API 키
     * @param adminKey Kakao 앱 관리자 키 (연동 해제 시 사용)
     */
    public record Kakao(
        String clientId,
        String adminKey
    ) {}

}
