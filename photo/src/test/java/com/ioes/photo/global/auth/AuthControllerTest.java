package com.ioes.photo.global.auth;

import com.ioes.photo.global.auth.dto.AppleLoginRequest;
import com.ioes.photo.global.auth.dto.KakaoLoginRequest;
import com.ioes.photo.global.auth.dto.LogoutRequest;
import com.ioes.photo.global.auth.dto.RefreshRequest;
import com.ioes.photo.global.auth.oauth.OAuthProvider;
import com.ioes.photo.global.auth.oauth.OAuthService;
import com.ioes.photo.global.auth.token.TokenResponse;
import com.ioes.photo.global.auth.token.TokenService;
import com.ioes.photo.global.common.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
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

    // ── kakaoLogin ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("kakaoLogin()")
    class KakaoLogin {

        @Test
        @DisplayName("accessToken으로 서비스를 호출하고 TokenResponse를 반환한다")
        void shouldReturnTokenResponse() {
            given(oAuthService.loginWithKakao("kakao-access-token"))
                .willReturn(buildTokenResponse(OAuthProvider.KAKAO));

            ApiResponse<TokenResponse> response = authController.kakaoLogin(
                new KakaoLoginRequest("kakao-access-token")
            );

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().accessToken()).isEqualTo("access-token");
            assertThat(response.getData().profile().provider()).isEqualTo(OAuthProvider.KAKAO);
        }
    }

    // ── appleLogin ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("appleLogin()")
    class AppleLogin {

        @Test
        @DisplayName("identityToken과 user 정보로 서비스를 호출하고 TokenResponse를 반환한다")
        void shouldReturnTokenResponse_withUserInfo() {
            AppleLoginRequest.AppleName name = new AppleLoginRequest.AppleName("John", "Doe");
            AppleLoginRequest.AppleUser user = new AppleLoginRequest.AppleUser(name, "john@icloud.com");
            AppleLoginRequest request = new AppleLoginRequest("apple-identity-token", user);
            given(oAuthService.loginWithApple(request)).willReturn(buildTokenResponse(OAuthProvider.APPLE));

            ApiResponse<TokenResponse> response = authController.appleLogin(request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().accessToken()).isEqualTo("access-token");
            assertThat(response.getData().profile().provider()).isEqualTo(OAuthProvider.APPLE);
        }

        @Test
        @DisplayName("user 정보 없이 identityToken만으로도 서비스를 호출한다 (재로그인)")
        void shouldReturnTokenResponse_withoutUserInfo() {
            AppleLoginRequest request = new AppleLoginRequest("apple-identity-token", null);
            given(oAuthService.loginWithApple(request)).willReturn(buildTokenResponse(OAuthProvider.APPLE));

            ApiResponse<TokenResponse> response = authController.appleLogin(request);

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
                new RefreshRequest("valid-refresh-token")
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
            MockHttpServletRequest httpRequest = new MockHttpServletRequest();

            ApiResponse<Void> response = authController.logout(
                new LogoutRequest("token-to-invalidate"),
                httpRequest
            );

            then(tokenService).should().logout(
                org.mockito.ArgumentMatchers.eq("token-to-invalidate"),
                org.mockito.ArgumentMatchers.isNull()
            );
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
