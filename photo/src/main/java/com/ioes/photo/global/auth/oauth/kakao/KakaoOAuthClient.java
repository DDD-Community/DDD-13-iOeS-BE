package com.ioes.photo.global.auth.oauth.kakao;

import com.ioes.photo.global.auth.oauth.OAuthClient;
import com.ioes.photo.global.auth.oauth.OAuthProvider;
import com.ioes.photo.global.auth.oauth.OAuthUserInfo;
import com.ioes.photo.global.config.oauth.properties.OAuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Kakao 로그인 OAuth 클라이언트 (전략 패턴 구현체).
 *
 * Kakao의 인증 흐름:
 * 1. 인증 URL 생성 -> 사용자를 Kakao 로그인 페이지로 리다이렉트
 * 2. Kakao가 GET 방식으로 code를 쿼리 파라미터로 전달
 * 3. code로 Kakao 토큰 엔드포인트 호출 -> access_token 획득
 * 4. access_token으로 사용자 정보 API 호출
 *
 *
 * 콜백 파라미터:
 * - Kakao 인증 코드 (필수)
 *
 * @see OAuthProperties.Kakao
 * @author 황제연
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoOAuthClient implements OAuthClient {

    private static final String KAKAO_AUTH_URL = "https://kauth.kakao.com/oauth/authorize";
    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;
    private final OAuthProperties oAuthProperties;

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.KAKAO;
    }

    /**
     * Kakao 로그인 인증 URL을 생성합니다.
     *
     * @return Kakao 인증 페이지 URL
     */
    @Override
    public String getAuthorizationUrl() {
        OAuthProperties.Kakao kakao = oAuthProperties.kakao();
        return KAKAO_AUTH_URL
            + "?response_type=code"
            + "&client_id=" + kakao.clientId()
            + "&redirect_uri=" + kakao.redirectUri();
    }

    /**
     * Kakao 인증 코드로 사용자 정보를 조회합니다
     *
     * @return 공통 사용자 정보
     */
    @Override
    public OAuthUserInfo getUserInfo(Map<String, String> params) {
        String code = params.get("code");

        KakaoTokenResponse tokenResponse = exchangeCodeForToken(code);
        KakaoUserInfoResponse userInfo = fetchUserInfo(tokenResponse.accessToken());

        String providerId = String.valueOf(userInfo.id());
        String email = userInfo.kakaoAccount() != null ? userInfo.kakaoAccount().email() : null;
        String nickname = null;
        String profileImgUrl = null;

        if (userInfo.kakaoAccount() != null && userInfo.kakaoAccount().profile() != null) {
            nickname = userInfo.kakaoAccount().profile().nickname();
            profileImgUrl = userInfo.kakaoAccount().profile().profileImageUrl();
        }

        log.debug("Kakao 사용자 정보 조회 완료: providerId={}", providerId);
        return new OAuthUserInfo(providerId, email, nickname, profileImgUrl, OAuthProvider.KAKAO);
    }

    private KakaoTokenResponse exchangeCodeForToken(String code) {
        OAuthProperties.Kakao kakao = oAuthProperties.kakao();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", kakao.clientId());
        form.add("client_secret", kakao.clientSecret());
        form.add("redirect_uri", kakao.redirectUri());
        form.add("code", code);

        return restClient.post()
            .uri(KAKAO_TOKEN_URL)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(KakaoTokenResponse.class);
    }

    private KakaoUserInfoResponse fetchUserInfo(String accessToken) {
        return restClient.get()
            .uri(KAKAO_USER_INFO_URL)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .body(KakaoUserInfoResponse.class);
    }
}