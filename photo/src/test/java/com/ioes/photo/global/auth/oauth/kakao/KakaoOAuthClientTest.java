package com.ioes.photo.global.auth.oauth.kakao;

import com.ioes.photo.global.auth.oauth.OAuthProvider;
import com.ioes.photo.global.auth.oauth.OAuthUserInfo;
import com.ioes.photo.global.config.oauth.properties.OAuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * {@link KakaoOAuthClient} 단위 테스트.
 *
 * HTTP 레이어는 MockRestServiceServer로 인터셉트하며,
 * 비즈니스 로직(URL 구성, 응답 파싱, 필드 매핑, revoke)을 검증합니다.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("KakaoOAuthClient 단위 테스트")
class KakaoOAuthClientTest {

    private static final String KAKAO_TOKEN_URL     = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String KAKAO_UNLINK_URL    = "https://kapi.kakao.com/v1/user/unlink";

    @Mock OAuthProperties oAuthProperties;

    private MockRestServiceServer server;
    private KakaoOAuthClient      kakaoOAuthClient;
    private OAuthProperties.Kakao kakaoProps;

    @BeforeEach
    void setUp() {
        kakaoProps = new OAuthProperties.Kakao(
            "test-client-id",
            "test-client-secret",
            "http://localhost/auth/oauth/kakao/callback",
            "test-admin-key"
        );
        given(oAuthProperties.kakao()).willReturn(kakaoProps);

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        kakaoOAuthClient = new KakaoOAuthClient(builder.build(), oAuthProperties);
    }

    // ── getProvider ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getProvider()는 KAKAO를 반환한다")
    void shouldReturnKakaoProvider() {
        assertThat(kakaoOAuthClient.getProvider()).isEqualTo(OAuthProvider.KAKAO);
    }

    // ── buildAuthorizationUrl ─────────────────────────────────────────────

    @Nested
    @DisplayName("buildAuthorizationUrl()")
    class BuildAuthorizationUrl {

        @Test
        @DisplayName("Kakao 인증 URL에 필수 파라미터가 포함된다")
        void shouldContainRequiredParams() {
            String url = kakaoOAuthClient.buildAuthorizationUrl("test-state", "test-challenge");

            assertThat(url)
                .contains("https://kauth.kakao.com/oauth/authorize")
                .contains("response_type=code")
                .contains("client_id=" + kakaoProps.clientId())
                .contains("redirect_uri=" + kakaoProps.redirectUri());
        }

        @Test
        @DisplayName("State와 PKCE code_challenge가 URL에 포함된다")
        void shouldContainStateAndPkce() {
            String url = kakaoOAuthClient.buildAuthorizationUrl("my-state", "my-challenge");

            assertThat(url)
                .contains("state=my-state")
                .contains("code_challenge=my-challenge")
                .contains("code_challenge_method=S256");
        }
    }

    // ── getUserInfo ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserInfo()")
    class GetUserInfo {

        @Test
        @DisplayName("인증 코드로 사용자 정보를 조회하면 OAuthUserInfo를 반환한다")
        void shouldReturnOAuthUserInfo() {
            server.expect(requestTo(KAKAO_TOKEN_URL)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(tokenJson("kakao-refresh-token"), MediaType.APPLICATION_JSON));
            server.expect(requestTo(KAKAO_USER_INFO_URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                    {"id":12345,"kakao_account":{"email":"test@kakao.com","email_verified":true,
                     "profile":{"nickname":"테스트유저","profile_image_url":"https://profile.kakao.com/image.jpg"}}}
                    """, MediaType.APPLICATION_JSON));

            OAuthUserInfo result = kakaoOAuthClient.getUserInfo(
                Map.of("code", "auth-code-123", "code_verifier", "verifier-xyz")
            );

            assertThat(result.provider()).isEqualTo(OAuthProvider.KAKAO);
            assertThat(result.providerId()).isEqualTo("12345");
            assertThat(result.email()).isEqualTo("test@kakao.com");
            assertThat(result.nickname()).isEqualTo("테스트유저");
            assertThat(result.profileImageUrl()).isEqualTo("https://profile.kakao.com/image.jpg");
            assertThat(result.providerRefreshToken()).isEqualTo("kakao-refresh-token");
        }

        @Test
        @DisplayName("카카오 계정 정보가 null이면 email, nickname, profileImageUrl이 null이다")
        void shouldHandleNullKakaoAccount() {
            server.expect(requestTo(KAKAO_TOKEN_URL)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(tokenJson("refresh"), MediaType.APPLICATION_JSON));
            server.expect(requestTo(KAKAO_USER_INFO_URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":99}", MediaType.APPLICATION_JSON));

            OAuthUserInfo result = kakaoOAuthClient.getUserInfo(Map.of("code", "code", "code_verifier", "v"));

            assertThat(result.providerId()).isEqualTo("99");
            assertThat(result.email()).isNull();
            assertThat(result.nickname()).isNull();
            assertThat(result.profileImageUrl()).isNull();
        }

        @Test
        @DisplayName("카카오 계정은 있지만 프로필이 null이면 nickname, profileImageUrl이 null이다")
        void shouldHandleNullProfile_whenAccountExists() {
            server.expect(requestTo(KAKAO_TOKEN_URL)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(tokenJson("refresh"), MediaType.APPLICATION_JSON));
            server.expect(requestTo(KAKAO_USER_INFO_URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                    {"id":77,"kakao_account":{"email":"email@kakao.com","email_verified":true}}
                    """, MediaType.APPLICATION_JSON));

            OAuthUserInfo result = kakaoOAuthClient.getUserInfo(Map.of("code", "code", "code_verifier", "v"));

            assertThat(result.email()).isEqualTo("email@kakao.com");
            assertThat(result.nickname()).isNull();
            assertThat(result.profileImageUrl()).isNull();
        }

        @Test
        @DisplayName("providerId는 Long 타입 id를 String으로 변환한다")
        void shouldConvertLongIdToString() {
            server.expect(requestTo(KAKAO_TOKEN_URL)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(tokenJson("refresh"), MediaType.APPLICATION_JSON));
            server.expect(requestTo(KAKAO_USER_INFO_URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":9876543210}", MediaType.APPLICATION_JSON));

            OAuthUserInfo result = kakaoOAuthClient.getUserInfo(Map.of("code", "code", "code_verifier", "v"));

            assertThat(result.providerId()).isEqualTo("9876543210");
        }
    }

    // ── revokeConnection ──────────────────────────────────────────────────

    @Nested
    @DisplayName("revokeConnection()")
    class RevokeConnection {

        @Test
        @DisplayName("관리자 키가 있으면 Kakao 연동 해제 API를 호출한다")
        void shouldCallUnlinkApi_whenAdminKeyPresent() {
            server.expect(requestTo(KAKAO_UNLINK_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "KakaoAK test-admin-key"))
                .andRespond(withSuccess("{\"id\":12345}", MediaType.APPLICATION_JSON));

            kakaoOAuthClient.revokeConnection("12345", null);

            server.verify();
        }

        @Test
        @DisplayName("관리자 키가 없으면 API 호출 없이 경고 로그만 남긴다")
        void shouldSkip_whenAdminKeyBlank() {
            OAuthProperties.Kakao propsNoAdminKey = new OAuthProperties.Kakao(
                "client-id", "client-secret", "http://redirect", null
            );
            given(oAuthProperties.kakao()).willReturn(propsNoAdminKey);

            // 서버 expectation 없음 → 호출되면 AssertionError
            kakaoOAuthClient.revokeConnection("12345", null);
        }
    }

    // ── helper ───────────────────────────────────────────────────────────

    private String tokenJson(String refreshToken) {
        return """
            {"token_type":"bearer","access_token":"kakao-access-token","expires_in":3600,
             "refresh_token":"%s","refresh_token_expires_in":5184000}
            """.formatted(refreshToken);
    }
}
