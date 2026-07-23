package com.ioes.photo.global.auth.oauth;

import com.ioes.photo.domain.user.entity.User;
import com.ioes.photo.domain.user.service.NicknameProperties;
import com.ioes.photo.domain.user.service.UserAccountService;
import com.ioes.photo.global.auth.dto.AppleLoginRequest;
import com.ioes.photo.global.auth.token.TokenResponse;
import com.ioes.photo.global.auth.token.TokenService;
import com.ioes.photo.global.config.security.JwtProvider;
import com.ioes.photo.global.error.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * {@link OAuthService} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OAuthService 단위 테스트")
class OAuthServiceTest {

    @Mock OAuthClientRegistry  registry;
    @Mock OAuthClient          oAuthClient;
    @Mock UserAccountService   userAccountService;
    @Mock TokenService         tokenService;
    @Mock NicknameProperties   nicknameProperties;
    @Mock JwtProvider          jwtProvider;

    @InjectMocks OAuthService oAuthService;

    private static final Long     USER_ID      = 1L;
    private static final String[] TEST_TOKENS  = {"access-token", "refresh-token"};

    @BeforeEach
    void setUpNicknameProperties() {
        NicknameProperties.Hashtag hashtag = new NicknameProperties.Hashtag();
        hashtag.setMaxAttempts(5);
        given(nicknameProperties.getHashtag()).willReturn(hashtag);
    }

    // ── loginWithKakao ────────────────────────────────────────────────────

    @Nested
    @DisplayName("loginWithKakao()")
    class LoginWithKakao {

        @BeforeEach
        void setUp() {
            given(registry.getClient(OAuthProvider.KAKAO)).willReturn(oAuthClient);
            given(tokenService.issueTokens(any(), any())).willReturn(TEST_TOKENS);
        }

        @Test
        @DisplayName("accessToken을 params에 담아 KakaoOAuthClient에 위임한다")
        void shouldDelegateWithAccessToken() {
            OAuthUserInfo userInfo = kakaoUserInfo("kakao-123");
            User user = createTestUser(userInfo);
            given(oAuthClient.getUserInfo(argThat(p -> "kakao-access-token".equals(p.get("accessToken")))))
                .willReturn(userInfo);
            given(userAccountService.findExistingUser(OAuthProvider.KAKAO, "kakao-123"))
                .willReturn(Optional.of(user));

            TokenResponse response = oAuthService.loginWithKakao("kakao-access-token");

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
        }

        @Test
        @DisplayName("신규 회원이면 createUser를 호출한다")
        void shouldCreateUser_whenNewUser() {
            OAuthUserInfo userInfo = kakaoUserInfo("kakao-new");
            User newUser = createTestUser(userInfo);
            given(oAuthClient.getUserInfo(any())).willReturn(userInfo);
            given(userAccountService.findExistingUser(OAuthProvider.KAKAO, "kakao-new"))
                .willReturn(Optional.empty());
            given(userAccountService.createUser(userInfo)).willReturn(newUser);

            oAuthService.loginWithKakao("any-token");

            then(userAccountService).should().createUser(userInfo);
        }

        @Test
        @DisplayName("기존 회원이면 createUser를 호출하지 않는다")
        void shouldNotCreateUser_whenUserExists() {
            OAuthUserInfo userInfo = kakaoUserInfo("kakao-existing");
            given(oAuthClient.getUserInfo(any())).willReturn(userInfo);
            given(userAccountService.findExistingUser(OAuthProvider.KAKAO, "kakao-existing"))
                .willReturn(Optional.of(createTestUser(userInfo)));

            oAuthService.loginWithKakao("any-token");

            then(userAccountService).should(never()).createUser(any());
        }

        @Test
        @DisplayName("로그인 응답에 프로필 정보가 포함된다")
        void shouldIncludeProfileInResponse() {
            OAuthUserInfo userInfo = kakaoUserInfo("kakao-123");
            given(oAuthClient.getUserInfo(any())).willReturn(userInfo);
            given(userAccountService.findExistingUser(any(), any()))
                .willReturn(Optional.of(createTestUser(userInfo)));

            TokenResponse response = oAuthService.loginWithKakao("token");

            assertThat(response.profile()).isNotNull();
            assertThat(response.profile().email()).isEqualTo("kakao@test.com");
            assertThat(response.profile().provider()).isEqualTo(OAuthProvider.KAKAO);
            assertThat(response.profile().userId()).isEqualTo(USER_ID.toString());
        }
    }

    // ── loginWithApple ────────────────────────────────────────────────────

    @Nested
    @DisplayName("loginWithApple()")
    class LoginWithApple {

        @BeforeEach
        void setUp() {
            given(registry.getClient(OAuthProvider.APPLE)).willReturn(oAuthClient);
            given(tokenService.issueTokens(any(), any())).willReturn(TEST_TOKENS);
        }

        @Test
        @DisplayName("identityToken을 params에 담아 AppleOAuthClient에 위임한다")
        void shouldDelegateWithIdentityToken() {
            OAuthUserInfo userInfo = appleUserInfo("apple-sub-001");
            given(oAuthClient.getUserInfo(argThat(p -> "test-identity-token".equals(p.get("identityToken")))))
                .willReturn(userInfo);
            given(userAccountService.findExistingUser(OAuthProvider.APPLE, "apple-sub-001"))
                .willReturn(Optional.of(createTestUser(userInfo)));

            AppleLoginRequest request = new AppleLoginRequest("test-identity-token", null);
            oAuthService.loginWithApple(request);

            then(oAuthClient).should().getUserInfo(argThat(p ->
                "test-identity-token".equals(p.get("identityToken"))
            ));
        }

        @Test
        @DisplayName("user 정보가 있으면 nickname을 params에 포함한다")
        void shouldIncludeNickname_whenUserPresent() {
            OAuthUserInfo userInfo = appleUserInfo("apple-sub-002");
            given(oAuthClient.getUserInfo(any())).willReturn(userInfo);
            given(userAccountService.findExistingUser(any(), any()))
                .willReturn(Optional.of(createTestUser(userInfo)));

            AppleLoginRequest.AppleName name = new AppleLoginRequest.AppleName("John", "Doe");
            AppleLoginRequest.AppleUser user = new AppleLoginRequest.AppleUser(name, "john@icloud.com");
            AppleLoginRequest request = new AppleLoginRequest("id-token", user);
            oAuthService.loginWithApple(request);

            then(oAuthClient).should().getUserInfo(argThat(p ->
                "John Doe".equals(p.get("nickname"))
            ));
        }

        @Test
        @DisplayName("user가 null이면 nickname params 없이 위임한다 (재로그인)")
        void shouldNotIncludeNickname_whenUserNull() {
            OAuthUserInfo userInfo = appleUserInfo("apple-sub-003");
            given(oAuthClient.getUserInfo(argThat(p -> !p.containsKey("nickname"))))
                .willReturn(userInfo);
            given(userAccountService.findExistingUser(any(), any()))
                .willReturn(Optional.of(createTestUser(userInfo)));

            AppleLoginRequest request = new AppleLoginRequest("id-token", null);
            oAuthService.loginWithApple(request);

            then(oAuthClient).should().getUserInfo(argThat(p -> !p.containsKey("nickname")));
        }

        @Test
        @DisplayName("신규 Apple 회원이면 createUser를 호출한다")
        void shouldCreateUser_whenNewAppleUser() {
            OAuthUserInfo userInfo = new OAuthUserInfo("apple-new", null, null, null, OAuthProvider.APPLE, null);
            User newUser = createUserWithHashTag("멋진코끼리", 7L);
            given(oAuthClient.getUserInfo(any())).willReturn(userInfo);
            given(userAccountService.findExistingUser(OAuthProvider.APPLE, "apple-new"))
                .willReturn(Optional.empty());
            given(userAccountService.createUser(userInfo)).willReturn(newUser);

            AppleLoginRequest request = new AppleLoginRequest("id-token", null);
            TokenResponse response = oAuthService.loginWithApple(request);

            then(userAccountService).should().createUser(userInfo);
            assertThat(response.profile().nickname()).isEqualTo("멋진코끼리#7");
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

    private OAuthUserInfo kakaoUserInfo(String providerId) {
        return new OAuthUserInfo(providerId, "kakao@test.com", "카카오유저", null, OAuthProvider.KAKAO, null);
    }

    private OAuthUserInfo appleUserInfo(String providerId) {
        return new OAuthUserInfo(providerId, "apple@test.com", "AppleUser", null, OAuthProvider.APPLE, null);
    }

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

    private User createUserWithHashTag(String nickname, Long hashTag) {
        User user = User.builder()
            .provider(OAuthProvider.APPLE)
            .providerUserId("apple-sub")
            .nickname(nickname)
            .hashTag(hashTag)
            .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }
}
