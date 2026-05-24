package com.ioes.photo.global.auth.oauth;

import java.util.Map;

/**
 * OAuth 공급자별 로그인 전략 인터페이스.
 *
 * 네이티브 SDK 플로우에서 각 공급자 구현체는 앱이 전달한 토큰을 검증하고
 * OAuthUserInfo를 반환합니다. 새로운 OAuth 공급자를 추가할 때 이 인터페이스를
 * 구현하고 Spring Bean으로 등록하면 OAuthClientRegistry가 자동으로 인식합니다.
 *
 * @author 황제연
 */
public interface OAuthClient {

    OAuthProvider getProvider();

    /**
     * 앱(네이티브 SDK)이 전달한 토큰 정보를 검증하고 사용자 정보를 반환합니다.
     *
     * <ul>
     *   <li>Kakao: params에 {@code accessToken} 포함</li>
     *   <li>Apple: params에 {@code identityToken} 포함, 최초 로그인 시 {@code nickname} 포함</li>
     * </ul>
     */
    OAuthUserInfo getUserInfo(Map<String, String> params);

    void revokeConnection(String providerUserId, String providerRefreshToken);
}
