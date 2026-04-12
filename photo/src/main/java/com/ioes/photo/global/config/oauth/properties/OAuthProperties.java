package com.ioes.photo.global.config.oauth.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth 공급자별 설정 프로퍼티.
 *
 * @param apple Apple OAuth 설정
 * @param kakao Kakao OAuth 설정
 * @author 황제연
 */
@ConfigurationProperties(prefix = "app.oauth")
public record OAuthProperties(Apple apple, Kakao kakao) {

    /**
     * Apple Sign In 설정.
     *
     * @param clientId Apple Services ID (Bundle ID)
     * @param teamId Apple Developer Team ID
     * @param keyId Apple Key ID
     * @param privateKey PKCS8 PEM 형식의 EC 비밀 키
     * @param redirectUri 인증 후 콜백 URI
     * @param clientSecretTtlMs client_secret JWT 유효 기간
     */
    public record Apple(
        String clientId,
        String teamId,
        String keyId,
        String privateKey,
        String redirectUri,
        long   clientSecretTtlMs
    ) {}

    /**
     * Kakao 로그인 설정
     *
     * @param clientId Kakao REST API 키
     * @param clientSecret Kakao 보안 코드
     * @param redirectUri 인증 후 콜백 URI
     * @param adminKey Kakao 앱 관리자 키 (연동 해제 시 사용, 카카오 개발자 콘솔 > 앱 키)
     */
    public record Kakao(
        String clientId,
        String clientSecret,
        String redirectUri,
        String adminKey
    ) {}
}