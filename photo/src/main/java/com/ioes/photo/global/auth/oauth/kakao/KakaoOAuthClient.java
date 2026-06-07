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
 * Kakao 네이티브 SDK 로그인 클라이언트 (전략 패턴 구현체).
 *
 * 네이티브 SDK 플로우:
 * 앱이 Kakao SDK로 발급받은 accessToken을 백엔드에 전달하면,
 * 백엔드가 해당 토큰으로 Kakao API를 호출하여 사용자 정보를 조회합니다.
 *
 * params 키:
 * - accessToken: Kakao SDK가 발급한 액세스 토큰 (필수)
 *
 * @see OAuthProperties.Kakao
 * @author 황제연
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoOAuthClient implements OAuthClient {

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String KAKAO_UNLINK_URL    = "https://kapi.kakao.com/v1/user/unlink";

    private final RestClient restClient;
    private final OAuthProperties oAuthProperties;

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.KAKAO;
    }

    @Override
    public OAuthUserInfo getUserInfo(Map<String, String> params) {
        String accessToken = params.get("accessToken");
        KakaoUserInfoResponse userInfo = fetchUserInfo(accessToken);

        String providerId    = String.valueOf(userInfo.id());
        String email         = userInfo.kakaoAccount() != null ? userInfo.kakaoAccount().email() : null;
        String nickname      = null;
        String profileImgUrl = null;

        if (userInfo.kakaoAccount() != null && userInfo.kakaoAccount().profile() != null) {
            nickname      = userInfo.kakaoAccount().profile().nickname();
            profileImgUrl = userInfo.kakaoAccount().profile().profileImageUrl();
        }

        log.debug("Kakao 사용자 정보 조회 완료: providerId={}", providerId);
        return new OAuthUserInfo(providerId, email, nickname, profileImgUrl, OAuthProvider.KAKAO, null);
    }

    @Override
    public void revokeConnection(String providerUserId, String providerRefreshToken) {
        OAuthProperties.Kakao kakao = oAuthProperties.kakao();
        if (kakao.adminKey() == null || kakao.adminKey().isBlank()) {
            log.warn("Kakao 관리자 키가 설정되지 않아 연동 해제를 건너뜁니다. (app.oauth.kakao.admin-key 설정 필요)");
            return;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("target_id_type", "user_id");
        form.add("target_id", providerUserId);

        restClient.post()
            .uri(KAKAO_UNLINK_URL)
            .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakao.adminKey())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .toBodilessEntity();

        log.info("Kakao 연동 해제 완료: providerUserId={}", providerUserId);
    }

    private KakaoUserInfoResponse fetchUserInfo(String accessToken) {
        return restClient.get()
            .uri(KAKAO_USER_INFO_URL)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .body(KakaoUserInfoResponse.class);
    }
}
