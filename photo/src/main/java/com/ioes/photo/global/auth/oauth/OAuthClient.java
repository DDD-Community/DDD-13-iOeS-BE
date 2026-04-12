package com.ioes.photo.global.auth.oauth;

import java.util.Map;

/**
 * OAuth 공급자별 로그인 전략 인터페이스
 *
 * 새로운 OAuth 공급자를 추가할 때 이 인터페이스를 구현하고 Spring Bean으로 등록하면
 * OAuthClientRegistry가 자동으로 인식합니다.
 * OAuthService나 OAuthClientRegistry를 수정할 필요가 없습니다.
 *
 * @author 황제연
 */
public interface OAuthClient {

    OAuthProvider getProvider();

    String buildAuthorizationUrl(String state, String codeChallenge);

    OAuthUserInfo getUserInfo(Map<String, String> params);

    void revokeConnection(String providerUserId, String providerRefreshToken);
}