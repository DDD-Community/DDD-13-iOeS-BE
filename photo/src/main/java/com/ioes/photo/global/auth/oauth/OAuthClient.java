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

    /*
     * 이 클라이언트가 처리하는 OAuth 공급자를 반환합니다.
     */
    OAuthProvider getProvider();

    /*
     * OAuth 인증 페이지 URL을 생성합니다.
     */
    String getAuthorizationUrl();

    /*
     * 인증 코드를 포함한 콜백 파라미터로 사용자 정보를 조회합니다.
     * 공급자마다 콜백 파라미터가 다를 수 있습니다.
     * Apple: code, user
     * Kakao: code
     * 따라서 Map을 통해 해당 문제를 해결했습니다.
     *
     * @param params 공급자가 콜백으로 전달한 모든 파라미터
     * @return 공통 사용자 정보
     */
    OAuthUserInfo getUserInfo(Map<String, String> params);
}