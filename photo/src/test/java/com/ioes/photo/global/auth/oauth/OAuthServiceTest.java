package com.ioes.photo.global.auth.oauth;

import com.ioes.photo.domain.user.entity.User;
import com.ioes.photo.domain.user.repository.UserRepository;
import com.ioes.photo.global.auth.token.TokenResponse;
import com.ioes.photo.global.auth.token.TokenService;
import com.ioes.photo.global.error.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * {@link OAuthService} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthService 단위 테스트")
class OAuthServiceTest {

    @Mock OAuthClientRegistry registry;
    @Mock OAuthClient         oAuthClient;
    @Mock UserRepository      userRepository;
    @Mock TokenService        tokenService;

    @InjectMocks OAuthService oAuthService;

    private static final String APPLE_AUTH_URL = "https://appleid.apple.com/auth/authorize?...";
    private static final String KAKAO_AUTH_URL = "https://kauth.kakao.com/oauth/authorize?...";
    private static final Long   USER_ID        = 1L;
    private static final String[] TEST_TOKENS  = {"access-token", "refresh-token"};

    // ── getAuthorizationUrl ───────────────────────────────────────────────

    @Nested
    @DisplayName("getAuthorizationUrl()")
    class GetAuthorizationUrl {

        @Test
        @DisplayName("APPLE 공급자이면 레지스트리에서 찾은 클라이언트에 위임한다")
        void shouldDelegateToAppleClient() {
            given(registry.getClient(OAuthProvider.APPLE)).willReturn(oAuthClient);
            given(oAuthClient.getAuthorizationUrl()).willReturn(APPLE_AUTH_URL);

            String url = oAuthService.getAuthorizationUrl(OAuthProvider.APPLE);

            assertThat(url).isEqualTo(APPLE_AUTH_URL);
        }

        @Test
        @DisplayName("KAKAO 공급자이면 레지스트리에서 찾은 클라이언트에 위임한다")
        void shouldDelegateToKakaoClient() {
            given(registry.getClient(OAuthProvider.KAKAO)).willReturn(oAuthClient);
            given(oAuthClient.getAuthorizationUrl()).willReturn(KAKAO_AUTH_URL);

            String url = oAuthService.getAuthorizationUrl(OAuthProvider.KAKAO);

            assertThat(url).isEqualTo(KAKAO_AUTH_URL);
        }
    }

    // ── handleCallback ────────────────────────────────────────────────────

    @Nested
    @DisplayName("handleCallback()")
    class HandleCallback {

        @BeforeEach
        void setUp() {
            given(registry.getClient(any())).willReturn(oAuthClient);
            given(tokenService.issueTokens(any())).willReturn(TEST_TOKENS);
        }

        @Test
        @DisplayName("기존 회원이 없으면 새로 생성하고 isNewUser=true를 반환한다")
        void shouldCreateNewUser_whenNotExists() {
            OAuthUserInfo userInfo = new OAuthUserInfo(
                "kakao-user-123", "kakao@test.com", "카카오유저", "https://img.kakao.com/profile.jpg",
                OAuthProvider.KAKAO
            );
            given(oAuthClient.getUserInfo(any())).willReturn(userInfo);
            given(userRepository.findByProviderAndProviderUserId(OAuthProvider.KAKAO, "kakao-user-123"))
                .willReturn(Optional.empty());
            given(userRepository.save(any(User.class))).willReturn(createTestUser(userInfo));

            TokenResponse response = oAuthService.handleCallback(OAuthProvider.KAKAO, Map.of("code", "kakao-code"));

            then(userRepository).should().save(any(User.class));
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            assertThat(response.profile()).isNotNull();
        }

        @Test
        @DisplayName("기존 회원이면 프로필을 업데이트하고 isNewUser=false를 반환한다")
        void shouldUpdateProfile_whenUserExists() {
            OAuthUserInfo userInfo = new OAuthUserInfo(
                "kakao-user-123", "kakao@test.com", "카카오유저", null,
                OAuthProvider.KAKAO
            );
            given(oAuthClient.getUserInfo(any())).willReturn(userInfo);
            given(userRepository.findByProviderAndProviderUserId(OAuthProvider.KAKAO, "kakao-user-123"))
                .willReturn(Optional.of(createTestUser(userInfo)));

            TokenResponse response = oAuthService.handleCallback(OAuthProvider.KAKAO, Map.of("code", "kakao-code"));

            then(userRepository).should(never()).save(any(User.class));
        }

        @Test
        @DisplayName("Apple 콜백을 처리하면 토큰 응답을 반환한다")
        void shouldHandleAppleCallback() {
            OAuthUserInfo appleUserInfo = new OAuthUserInfo(
                "apple-sub-123", "apple@test.com", "AppleUser", null,
                OAuthProvider.APPLE
            );
            given(oAuthClient.getUserInfo(any())).willReturn(appleUserInfo);
            given(userRepository.findByProviderAndProviderUserId(OAuthProvider.APPLE, "apple-sub-123"))
                .willReturn(Optional.of(createTestUser(appleUserInfo)));

            TokenResponse response = oAuthService.handleCallback(
                OAuthProvider.APPLE,
                Map.of("code", "apple-code")
            );

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.profile().provider()).isEqualTo(OAuthProvider.APPLE);
        }

        @Test
        @DisplayName("Apple 신규 회원가입이면 유저를 저장한다")
        void shouldSaveUser_forAppleSignUp() {
            OAuthUserInfo appleUserInfo = new OAuthUserInfo(
                "apple-new-sub", "new@apple.com", "새사용자", null,
                OAuthProvider.APPLE
            );
            given(oAuthClient.getUserInfo(any())).willReturn(appleUserInfo);
            given(userRepository.findByProviderAndProviderUserId(OAuthProvider.APPLE, "apple-new-sub"))
                .willReturn(Optional.empty());
            given(userRepository.save(any(User.class))).willReturn(createTestUser(appleUserInfo));

            oAuthService.handleCallback(OAuthProvider.APPLE, Map.of("code", "code"));

            then(userRepository).should().save(any(User.class));
        }

        @Test
        @DisplayName("로그인 응답에 프로필 정보가 포함된다")
        void shouldIncludeProfileInResponse() {
            OAuthUserInfo userInfo = new OAuthUserInfo(
                "kakao-user-123", "kakao@test.com", "카카오유저", null,
                OAuthProvider.KAKAO
            );
            given(oAuthClient.getUserInfo(any())).willReturn(userInfo);
            given(userRepository.findByProviderAndProviderUserId(any(), any()))
                .willReturn(Optional.of(createTestUser(userInfo)));

            TokenResponse response = oAuthService.handleCallback(OAuthProvider.KAKAO, Map.of("code", "code"));

            assertThat(response.profile()).isNotNull();
            assertThat(response.profile().email()).isEqualTo("kakao@test.com");
            assertThat(response.profile().provider()).isEqualTo(OAuthProvider.KAKAO);
            assertThat(response.profile().userId()).isEqualTo(USER_ID.toString());
        }
    }

    // ── resolveProvider ───────────────────────────────────────────────────

    @Nested
    @DisplayName("resolveProvider()")
    class ResolveProvider {

        @Test
        @DisplayName("'apple' 문자열을 APPLE 열거형으로 변환한다")
        void shouldResolveApple() {
            assertThat(oAuthService.resolveProvider("apple")).isEqualTo(OAuthProvider.APPLE);
        }

        @Test
        @DisplayName("'KAKAO' 대문자 문자열을 KAKAO 열거형으로 변환한다")
        void shouldResolveKakaoUpperCase() {
            assertThat(oAuthService.resolveProvider("KAKAO")).isEqualTo(OAuthProvider.KAKAO);
        }

        @Test
        @DisplayName("'Kakao' 혼합 대소문자도 변환된다")
        void shouldResolveCaseInsensitive() {
            assertThat(oAuthService.resolveProvider("Kakao")).isEqualTo(OAuthProvider.KAKAO);
        }

        @Test
        @DisplayName("지원하지 않는 공급자면 BusinessException을 던진다")
        void shouldThrow_whenInvalidProvider() {
            assertThatThrownBy(() -> oAuthService.resolveProvider("naver"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("naver");
        }

        @Test
        @DisplayName("빈 문자열이면 BusinessException을 던진다")
        void shouldThrow_whenEmptyProvider() {
            assertThatThrownBy(() -> oAuthService.resolveProvider(""))
                .isInstanceOf(BusinessException.class);
        }
    }

    // ── helper ───────────────────────────────────────────────────────────

    private User createTestUser(OAuthUserInfo info) {
        User user = User.builder()
            .provider(info.provider())
            .providerUserId(info.providerId())
            .email(info.email())
            .nickname(info.nickname())
            .profileImageUrl(info.profileImageUrl())
            .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }
}
