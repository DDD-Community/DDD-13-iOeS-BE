package com.ioes.photo.global.auth;

import com.ioes.photo.global.auth.oauth.OAuthProvider;
import com.ioes.photo.global.auth.oauth.OAuthService;
import com.ioes.photo.global.auth.token.TokenResponse;
import com.ioes.photo.global.auth.token.TokenService;
import com.ioes.photo.global.common.response.ApiResponse;
import com.ioes.photo.global.error.exception.BusinessException;
import com.ioes.photo.global.error.code.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * {@link AuthController} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController 단위 테스트")
class AuthControllerTest {

    @Mock OAuthService oAuthService;
    @Mock TokenService tokenService;

    @InjectMocks AuthController authController;

    private static final String KAKAO_AUTH_URL = "https://kauth.kakao.com/oauth/authorize?...";
    private static final String APPLE_AUTH_URL = "https://appleid.apple.com/auth/authorize?...";

    // ── getAuthorizationUrl ───────────────────────────────────────────────

    @Nested
    @DisplayName("getAuthorizationUrl()")
    class GetAuthorizationUrl {

        @Test
        @DisplayName("kakao 공급자이면 Kakao 인증 URL을 응답한다")
        void shouldReturnKakaoUrl() {
            given(oAuthService.resolveProvider("kakao")).willReturn(OAuthProvider.KAKAO);
            given(oAuthService.getAuthorizationUrl(OAuthProvider.KAKAO)).willReturn(KAKAO_AUTH_URL);

            ApiResponse<Map<String, String>> response = authController.getAuthorizationUrl("kakao");

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).containsEntry("authorizationUrl", KAKAO_AUTH_URL);
        }

        @Test
        @DisplayName("apple 공급자이면 Apple 인증 URL을 응답한다")
        void shouldReturnAppleUrl() {
            given(oAuthService.resolveProvider("apple")).willReturn(OAuthProvider.APPLE);
            given(oAuthService.getAuthorizationUrl(OAuthProvider.APPLE)).willReturn(APPLE_AUTH_URL);

            ApiResponse<Map<String, String>> response = authController.getAuthorizationUrl("apple");

            assertThat(response.getData()).containsEntry("authorizationUrl", APPLE_AUTH_URL);
        }

        @Test
        @DisplayName("지원하지 않는 공급자이면 예외가 전파된다")
        void shouldPropagateException_whenInvalidProvider() {
            given(oAuthService.resolveProvider("naver"))
                .willThrow(new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 OAuth 공급자"));

            assertThatThrownBy(() -> authController.getAuthorizationUrl("naver"))
                .isInstanceOf(BusinessException.class);
        }
    }

    // ── oauthCallback (GET) ───────────────────────────────────────────────

    @Nested
    @DisplayName("oauthCallback() — GET 기반 공급자")
    class OAuthCallback {

        @Test
        @DisplayName("Kakao 인증 코드로 토큰 응답을 반환한다")
        void shouldReturnTokenResponse_forKakao() {
            Map<String, String> params = Map.of("code", "kakao-code");
            TokenResponse tokenResponse = buildTokenResponse(OAuthProvider.KAKAO);
            given(oAuthService.resolveProvider("kakao")).willReturn(OAuthProvider.KAKAO);
            given(oAuthService.handleCallback(OAuthProvider.KAKAO, params)).willReturn(tokenResponse);

            ApiResponse<TokenResponse> response = authController.oauthCallback("kakao", params);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().accessToken()).isEqualTo("access-token");
            assertThat(response.getData().refreshToken()).isEqualTo("refresh-token");
        }
    }

    // ── appleCallback (POST) ──────────────────────────────────────────────

    @Nested
    @DisplayName("appleCallback() — Apple form_post")
    class AppleCallback {

        @Test
        @DisplayName("Apple 인증 코드와 user 파라미터로 토큰 응답을 반환한다")
        void shouldReturnTokenResponse_withUserParam() {
            String userJson = "{\"name\":{\"firstName\":\"John\",\"lastName\":\"Doe\"}}";
            Map<String, String> params = Map.of("code", "apple-code", "user", userJson);
            TokenResponse tokenResponse = buildTokenResponse(OAuthProvider.APPLE);
            given(oAuthService.handleCallback(eq(OAuthProvider.APPLE), eq(params))).willReturn(tokenResponse);

            ApiResponse<TokenResponse> response = authController.appleCallback(params);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().accessToken()).isEqualTo("access-token");
        }

        @Test
        @DisplayName("user 파라미터 없이도 토큰 응답을 반환한다 (재로그인)")
        void shouldReturnTokenResponse_withoutUserParam() {
            Map<String, String> params = Map.of("code", "apple-code");
            TokenResponse tokenResponse = buildTokenResponse(OAuthProvider.APPLE);
            given(oAuthService.handleCallback(eq(OAuthProvider.APPLE), eq(params))).willReturn(tokenResponse);

            ApiResponse<TokenResponse> response = authController.appleCallback(params);

            assertThat(response.isSuccess()).isTrue();
        }
    }

    // ── refresh ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("refresh()")
    class Refresh {

        @Test
        @DisplayName("유효한 Refresh Token으로 새 토큰 쌍을 반환한다")
        void shouldReturnNewTokenPair() {
            given(tokenService.refreshTokens("valid-refresh-token"))
                .willReturn(new String[]{"new-access", "new-refresh"});

            ApiResponse<TokenResponse> response = authController.refresh(
                Map.of("refreshToken", "valid-refresh-token")
            );

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().accessToken()).isEqualTo("new-access");
            assertThat(response.getData().refreshToken()).isEqualTo("new-refresh");
            assertThat(response.getData().profile()).isNull();
        }
    }

    // ── logout ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("Refresh Token 무효화 후 성공 응답을 반환한다")
        void shouldReturnSuccess() {
            ApiResponse<Void> response = authController.logout(
                Map.of("refreshToken", "token-to-invalidate")
            );

            then(tokenService).should().invalidateRefreshToken("token-to-invalidate");
            assertThat(response.isSuccess()).isTrue();
        }
    }

    // ── helper ───────────────────────────────────────────────────────────

    private TokenResponse buildTokenResponse(OAuthProvider provider) {
        TokenResponse.UserProfile profile = new TokenResponse.UserProfile(
            "user-id", "test@test.com", "테스터", null, provider
        );
        return new TokenResponse("access-token", "refresh-token", profile);
    }
}