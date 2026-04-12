package com.ioes.photo.global.auth.oauth;

import com.ioes.photo.domain.user.entity.User;
import com.ioes.photo.domain.user.repository.UserRepository;
import com.ioes.photo.domain.user.service.NicknameGenerator;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    @Mock OAuthStateStore     stateStore;
    @Mock OAuthClient         oAuthClient;
    @Mock UserRepository      userRepository;
    @Mock TokenService        tokenService;
    @Mock NicknameGenerator   nicknameGenerator;

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
        @DisplayName("APPLE 공급자이면 state와 PKCE가 포함된 URL을 반환한다")
        void shouldDelegateToAppleClient() {
            given(registry.getClient(OAuthProvider.APPLE)).willReturn(oAuthClient);
            given(oAuthClient.buildAuthorizationUrl(anyString(), anyString())).willReturn(APPLE_AUTH_URL);

            String url = oAuthService.getAuthorizationUrl(OAuthProvider.APPLE);

            assertThat(url).isEqualTo(APPLE_AUTH_URL);
            then(stateStore).should().save(anyString(), anyString());
        }

        @Test
        @DisplayName("KAKAO 공급자이면 state와 PKCE가 포함된 URL을 반환한다")
        void shouldDelegateToKakaoClient() {
            given(registry.getClient(OAuthProvider.KAKAO)).willReturn(oAuthClient);
            given(oAuthClient.buildAuthorizationUrl(anyString(), anyString())).willReturn(KAKAO_AUTH_URL);

            String url = oAuthService.getAuthorizationUrl(OAuthProvider.KAKAO);

            assertThat(url).isEqualTo(KAKAO_AUTH_URL);
        }

        @Test
        @DisplayName("매 호출마다 서로 다른 state가 생성되어 저장된다")
        void shouldGenerateUniqueStateEachCall() {
            given(registry.getClient(any())).willReturn(oAuthClient);
            given(oAuthClient.buildAuthorizationUrl(anyString(), anyString())).willReturn(KAKAO_AUTH_URL);

            oAuthService.getAuthorizationUrl(OAuthProvider.KAKAO);
            oAuthService.getAuthorizationUrl(OAuthProvider.KAKAO);

            // save가 2번 호출되었어야 한다
            then(stateStore).should(org.mockito.Mockito.times(2)).save(anyString(), anyString());
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
            given(stateStore.getAndDelete("test-state")).willReturn("code-verifier-value");
        }

        private Map<String, String> paramsWithState(String... extra) {
            Map<String, String> params = new HashMap<>();
            params.put("code", "test-code");
            params.put("state", "test-state");
            for (int i = 0; i < extra.length; i += 2) {
                params.put(extra[i], extra[i + 1]);
            }
            return params;
        }

        @Test
        @DisplayName("기존 회원이 없으면 새로 생성하고 토큰을 반환한다")
        void shouldCreateNewUser_whenNotExists() {
            OAuthUserInfo userInfo = kakaoUserInfo("kakao-user-123");
            given(oAuthClient.getUserInfo(any())).willReturn(userInfo);
            given(userRepository.findByProviderAndProviderUserId(OAuthProvider.KAKAO, "kakao-user-123"))
                .willReturn(Optional.empty());
            given(userRepository.save(any(User.class))).willReturn(createTestUser(userInfo));

            TokenResponse response = oAuthService.handleCallback(OAuthProvider.KAKAO, paramsWithState());

            then(userRepository).should().save(any(User.class));
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
        }

        @Test
        @DisplayName("기존 회원이면 프로필을 업데이트하고 save를 호출하지 않는다")
        void shouldUpdateProfile_whenUserExists() {
            OAuthUserInfo userInfo = kakaoUserInfo("kakao-user-123");
            given(oAuthClient.getUserInfo(any())).willReturn(userInfo);
            given(userRepository.findByProviderAndProviderUserId(OAuthProvider.KAKAO, "kakao-user-123"))
                .willReturn(Optional.of(createTestUser(userInfo)));

            oAuthService.handleCallback(OAuthProvider.KAKAO, paramsWithState());

            then(userRepository).should(never()).save(any(User.class));
        }

        @Test
        @DisplayName("Apple 콜백을 처리하면 토큰 응답을 반환한다")
        void shouldHandleAppleCallback() {
            OAuthUserInfo appleUserInfo = appleUserInfo("apple-sub-123");
            given(oAuthClient.getUserInfo(any())).willReturn(appleUserInfo);
            given(userRepository.findByProviderAndProviderUserId(OAuthProvider.APPLE, "apple-sub-123"))
                .willReturn(Optional.of(createTestUser(appleUserInfo)));

            TokenResponse response = oAuthService.handleCallback(
                OAuthProvider.APPLE, paramsWithState()
            );

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.profile().provider()).isEqualTo(OAuthProvider.APPLE);
        }

        @Test
        @DisplayName("로그인 응답에 프로필 정보가 포함된다")
        void shouldIncludeProfileInResponse() {
            OAuthUserInfo userInfo = kakaoUserInfo("kakao-user-123");
            given(oAuthClient.getUserInfo(any())).willReturn(userInfo);
            given(userRepository.findByProviderAndProviderUserId(any(), any()))
                .willReturn(Optional.of(createTestUser(userInfo)));

            TokenResponse response = oAuthService.handleCallback(OAuthProvider.KAKAO, paramsWithState());

            assertThat(response.profile()).isNotNull();
            assertThat(response.profile().email()).isEqualTo("kakao@test.com");
            assertThat(response.profile().provider()).isEqualTo(OAuthProvider.KAKAO);
            assertThat(response.profile().userId()).isEqualTo(USER_ID.toString());
        }

        @Test
        @DisplayName("providerRefreshToken이 있으면 Redis(TokenService)에 저장한다")
        void shouldStoreProviderRefreshToken_whenPresent() {
            OAuthUserInfo userInfo = new OAuthUserInfo(
                "kakao-123", "k@k.com", "유저", null, OAuthProvider.KAKAO, "provider-refresh-token"
            );
            User user = createTestUser(userInfo);
            given(oAuthClient.getUserInfo(any())).willReturn(userInfo);
            given(userRepository.findByProviderAndProviderUserId(any(), any()))
                .willReturn(Optional.of(user));

            oAuthService.handleCallback(OAuthProvider.KAKAO, paramsWithState());

            then(tokenService).should().storeProviderRefreshToken(USER_ID.toString(), "provider-refresh-token");
        }
    }

    // ── handleCallback — state 검증 ────────────────────────────────────────

    @Nested
    @DisplayName("handleCallback() — State 검증")
    class StateValidation {

        @Test
        @DisplayName("state 파라미터가 없으면 UNAUTHORIZED 예외를 던진다")
        void shouldThrow_whenStateMissing() {
            Map<String, String> params = Map.of("code", "some-code");

            assertThatThrownBy(() -> oAuthService.handleCallback(OAuthProvider.KAKAO, params))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("state");
        }

        @Test
        @DisplayName("Redis에 없는 state이면 UNAUTHORIZED 예외를 던진다 (만료 또는 위조)")
        void shouldThrow_whenStateNotInRedis() {
            given(stateStore.getAndDelete("unknown-state")).willReturn(null);
            Map<String, String> params = Map.of("code", "code", "state", "unknown-state");

            assertThatThrownBy(() -> oAuthService.handleCallback(OAuthProvider.KAKAO, params))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("state");
        }

        @Test
        @DisplayName("state 검증 후 code_verifier가 파라미터에 주입되어 클라이언트에 전달된다")
        void shouldInjectCodeVerifierIntoParams() {
            given(stateStore.getAndDelete("valid-state")).willReturn("verifier-xyz");
            given(registry.getClient(any())).willReturn(oAuthClient);
            given(tokenService.issueTokens(any())).willReturn(TEST_TOKENS);

            OAuthUserInfo userInfo = kakaoUserInfo("kakao-123");
            given(oAuthClient.getUserInfo(any())).willReturn(userInfo);
            given(userRepository.findByProviderAndProviderUserId(any(), any()))
                .willReturn(Optional.of(createTestUser(userInfo)));

            Map<String, String> params = Map.of("code", "code", "state", "valid-state");
            oAuthService.handleCallback(OAuthProvider.KAKAO, params);

            // getUserInfo가 code_verifier를 포함한 enriched params로 호출됨을 검증
            then(oAuthClient).should().getUserInfo(
                org.mockito.ArgumentMatchers.argThat(p ->
                    "verifier-xyz".equals(p.get("code_verifier"))
                )
            );
        }
    }

    // ── handleCallback — 닉네임 자동 생성 ─────────────────────────────────

    @Nested
    @DisplayName("handleCallback() - 닉네임 자동 생성")
    class HandleCallbackNicknameGeneration {

        @BeforeEach
        void setUp() {
            given(registry.getClient(any())).willReturn(oAuthClient);
            given(tokenService.issueTokens(any())).willReturn(TEST_TOKENS);
            given(stateStore.getAndDelete(anyString())).willReturn("code-verifier");
        }

        private Map<String, String> params() {
            return Map.of("code", "code", "state", "state");
        }

        @Test
        @DisplayName("nickname이 null이면 NicknameGenerator를 호출한다")
        void shouldCallNicknameGenerator_whenNicknameIsNull() {
            OAuthUserInfo userInfo = new OAuthUserInfo("apple-sub-001", null, null, null, OAuthProvider.APPLE, null);
            given(oAuthClient.getUserInfo(any())).willReturn(userInfo);
            given(userRepository.findByProviderAndProviderUserId(OAuthProvider.APPLE, "apple-sub-001"))
                .willReturn(Optional.empty());
            given(nicknameGenerator.maxAttempts()).willReturn(3);
            given(nicknameGenerator.generate()).willReturn(new NicknameGenerator.Result("멋진코끼리", 7L));
            given(userRepository.save(any(User.class))).willReturn(createUserWithHashTag("멋진코끼리", 7L));

            oAuthService.handleCallback(OAuthProvider.APPLE, params());

            then(nicknameGenerator).should().generate();
        }

        @Test
        @DisplayName("nickname이 있으면 NicknameGenerator를 호출하지 않는다")
        void shouldNotCallNicknameGenerator_whenNicknameExists() {
            OAuthUserInfo userInfo = new OAuthUserInfo(
                "kakao-user-111", "kakao@test.com", "카카오유저", null, OAuthProvider.KAKAO, null
            );
            given(oAuthClient.getUserInfo(any())).willReturn(userInfo);
            given(userRepository.findByProviderAndProviderUserId(OAuthProvider.KAKAO, "kakao-user-111"))
                .willReturn(Optional.empty());
            given(userRepository.save(any(User.class))).willReturn(createTestUser(userInfo));

            oAuthService.handleCallback(OAuthProvider.KAKAO, params());

            then(nicknameGenerator).should(never()).generate();
        }

        @Test
        @DisplayName("자동 생성된 닉네임은 nickname#hashTag 형식으로 응답에 포함된다")
        void shouldIncludeDisplayNameInResponse_whenNicknameGenerated() {
            OAuthUserInfo userInfo = new OAuthUserInfo("apple-sub-002", null, null, null, OAuthProvider.APPLE, null);
            given(oAuthClient.getUserInfo(any())).willReturn(userInfo);
            given(userRepository.findByProviderAndProviderUserId(OAuthProvider.APPLE, "apple-sub-002"))
                .willReturn(Optional.empty());
            given(nicknameGenerator.maxAttempts()).willReturn(3);
            given(nicknameGenerator.generate()).willReturn(new NicknameGenerator.Result("포근한여우", 21L));
            given(userRepository.save(any(User.class))).willReturn(createUserWithHashTag("포근한여우", 21L));

            TokenResponse response = oAuthService.handleCallback(OAuthProvider.APPLE, params());

            assertThat(response.profile().nickname()).isEqualTo("포근한여우#21");
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

    private OAuthUserInfo kakaoUserInfo(String providerId) {
        return new OAuthUserInfo(providerId, "kakao@test.com", "카카오유저", null, OAuthProvider.KAKAO, "kakao-refresh");
    }

    private OAuthUserInfo appleUserInfo(String providerId) {
        return new OAuthUserInfo(providerId, "apple@test.com", "AppleUser", null, OAuthProvider.APPLE, "apple-refresh");
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
