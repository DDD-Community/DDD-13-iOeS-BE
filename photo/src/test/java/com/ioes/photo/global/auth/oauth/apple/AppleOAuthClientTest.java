package com.ioes.photo.global.auth.oauth.apple;

import com.ioes.photo.global.auth.oauth.OAuthProvider;
import com.ioes.photo.global.auth.oauth.OAuthUserInfo;
import com.ioes.photo.global.config.oauth.properties.OAuthProperties;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * {@link AppleOAuthClient} 단위 테스트.
 *
 * Apple은 실제 키 발급 없이 테스트할 수 없으므로 단위 테스트로 최대한 검증합니다.
 *  - ID token 파싱 (서명 검증 없이 payload 추출)
 *  - user JSON에서 닉네임 추출 (최초 로그인 케이스)
 *  - 인증 URL 파라미터 검증
 *  - 잘못된 키/토큰 시 예외 처리
 *
 * HTTP 레이어는 MockRestServiceServer로 인터셉트합니다.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AppleOAuthClient 단위 테스트")
class AppleOAuthClientTest {

    private static final String APPLE_TOKEN_URL = "https://appleid.apple.com/auth/token";

    @Mock OAuthProperties oAuthProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockRestServiceServer server;
    private AppleOAuthClient appleOAuthClient;
    private OAuthProperties.Apple appleProps;
    private String validPrivateKeyPem;

    @BeforeEach
    void setUp() throws Exception {
        // 테스트용 P-256 EC 키 동적 생성 (Apple ES256 서명에 사용하는 알고리즘)
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = kpg.generateKeyPair();
        validPrivateKeyPem = "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----";

        appleProps = new OAuthProperties.Apple(
            "com.test.app",
            "TEST1TEAM1",
            "TESTKEY123",
            validPrivateKeyPem,
            "https://test.com/auth/oauth/apple/callback",
            15552000000L
        );
        given(oAuthProperties.apple()).willReturn(appleProps);

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        appleOAuthClient = new AppleOAuthClient(builder.build(), objectMapper, oAuthProperties);
    }

    // ── getProvider ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getProvider()는 APPLE을 반환한다")
    void shouldReturnAppleProvider() {
        assertThat(appleOAuthClient.getProvider()).isEqualTo(OAuthProvider.APPLE);
    }

    // ── getAuthorizationUrl ───────────────────────────────────────────────

    @Nested
    @DisplayName("getAuthorizationUrl()")
    class GetAuthorizationUrl {

        @Test
        @DisplayName("Apple 인증 URL에 필수 파라미터가 포함된다")
        void shouldContainRequiredParams() {
            String url = appleOAuthClient.getAuthorizationUrl();

            assertThat(url)
                .contains("https://appleid.apple.com/auth/authorize")
                .contains("response_type=code")
                .contains("client_id=" + appleProps.clientId())
                .contains("redirect_uri=" + appleProps.redirectUri())
                .contains("scope=name%20email")
                .contains("response_mode=form_post");
        }

        @Test
        @DisplayName("response_mode=form_post가 포함된다 (Apple POST 콜백 방식)")
        void shouldUseFormPost() {
            String url = appleOAuthClient.getAuthorizationUrl();

            assertThat(url).contains("response_mode=form_post");
        }
    }

    // ── getUserInfo ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserInfo() — ID token 파싱")
    class GetUserInfo {

        @Test
        @DisplayName("Apple ID 토큰에서 sub와 email을 추출한다")
        void shouldExtractSubAndEmailFromIdToken() {
            String idToken = buildTestIdToken("apple-user-sub-001", "apple@test.com");
            server.expect(requestTo(APPLE_TOKEN_URL)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(appleTokenJson(idToken), MediaType.APPLICATION_JSON));

            OAuthUserInfo result = appleOAuthClient.getUserInfo(Map.of("code", "auth-code"));

            assertThat(result.provider()).isEqualTo(OAuthProvider.APPLE);
            assertThat(result.providerId()).isEqualTo("apple-user-sub-001");
            assertThat(result.email()).isEqualTo("apple@test.com");
            assertThat(result.profileImageUrl()).isNull(); // Apple은 프로필 이미지 미제공
        }

        @Test
        @DisplayName("이메일이 없는 ID 토큰도 처리한다")
        void shouldHandleIdTokenWithoutEmail() {
            String idToken = buildTestIdToken("sub-no-email", null);
            server.expect(requestTo(APPLE_TOKEN_URL)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(appleTokenJson(idToken), MediaType.APPLICATION_JSON));

            OAuthUserInfo result = appleOAuthClient.getUserInfo(Map.of("code", "code"));

            assertThat(result.providerId()).isEqualTo("sub-no-email");
            assertThat(result.email()).isNull();
        }

        @Test
        @DisplayName("유효하지 않은 JWT 형식이면 BusinessException(UNAUTHORIZED)을 던진다")
        void shouldThrow_whenInvalidIdToken() {
            server.expect(requestTo(APPLE_TOKEN_URL)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(appleTokenJson("not-a-jwt"), MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> appleOAuthClient.getUserInfo(Map.of("code", "code")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(CommonErrorCode.UNAUTHORIZED));
        }

        @Test
        @DisplayName("payload가 유효한 JSON이 아니면 BusinessException(UNAUTHORIZED)을 던진다")
        void shouldThrow_whenPayloadIsNotJson() {
            String badToken = base64Url("{header}") + "." + base64Url("not-json") + ".sig";
            server.expect(requestTo(APPLE_TOKEN_URL)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(appleTokenJson(badToken), MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> appleOAuthClient.getUserInfo(Map.of("code", "code")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(CommonErrorCode.UNAUTHORIZED));
        }
    }

    // ── getUserInfo — user JSON 닉네임 추출 ───────────────────────────────

    @Nested
    @DisplayName("getUserInfo() — user JSON 닉네임 추출 (최초 로그인)")
    class NicknameExtraction {

        @BeforeEach
        void setUpServer() {
            String idToken = buildTestIdToken("sub-abc", "user@icloud.com");
            server.expect(requestTo(APPLE_TOKEN_URL)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(appleTokenJson(idToken), MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("firstName + lastName이 모두 있으면 공백으로 합쳐 반환한다")
        void shouldCombineFirstAndLastName() {
            String userJson = "{\"name\":{\"firstName\":\"John\",\"lastName\":\"Doe\"}}";

            OAuthUserInfo result = appleOAuthClient.getUserInfo(Map.of("code", "code", "user", userJson));

            assertThat(result.nickname()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("firstName만 있으면 firstName을 반환한다")
        void shouldUseFirstNameOnly_whenLastNameNull() {
            String userJson = "{\"name\":{\"firstName\":\"John\"}}";

            OAuthUserInfo result = appleOAuthClient.getUserInfo(Map.of("code", "code", "user", userJson));

            assertThat(result.nickname()).isEqualTo("John");
        }

        @Test
        @DisplayName("lastName만 있으면 lastName을 반환한다")
        void shouldUseLastNameOnly_whenFirstNameNull() {
            String userJson = "{\"name\":{\"lastName\":\"Doe\"}}";

            OAuthUserInfo result = appleOAuthClient.getUserInfo(Map.of("code", "code", "user", userJson));

            assertThat(result.nickname()).isEqualTo("Doe");
        }

        @Test
        @DisplayName("name 필드가 없으면 null을 반환한다")
        void shouldReturnNull_whenNameFieldMissing() {
            String userJson = "{\"email\":\"user@icloud.com\"}";

            OAuthUserInfo result = appleOAuthClient.getUserInfo(Map.of("code", "code", "user", userJson));

            assertThat(result.nickname()).isNull();
        }

        @Test
        @DisplayName("name 오브젝트가 비어 있으면 null을 반환한다")
        void shouldReturnNull_whenNameIsEmptyObject() {
            String userJson = "{\"name\":{}}";

            OAuthUserInfo result = appleOAuthClient.getUserInfo(Map.of("code", "code", "user", userJson));

            assertThat(result.nickname()).isNull();
        }

        @Test
        @DisplayName("재로그인 시 user 파라미터가 없으면 nickname이 null이다")
        void shouldReturnNullNickname_whenUserParamAbsent() {
            OAuthUserInfo result = appleOAuthClient.getUserInfo(Map.of("code", "code"));

            assertThat(result.nickname()).isNull();
        }

        @Test
        @DisplayName("user JSON이 빈 문자열이면 nickname이 null이다")
        void shouldReturnNullNickname_whenUserParamBlank() {
            OAuthUserInfo result = appleOAuthClient.getUserInfo(Map.of("code", "code", "user", ""));

            assertThat(result.nickname()).isNull();
        }

        @Test
        @DisplayName("user JSON이 잘못된 형식이면 예외 없이 null을 반환한다")
        void shouldReturnNull_whenUserJsonMalformed() {
            OAuthUserInfo result = appleOAuthClient.getUserInfo(
                Map.of("code", "code", "user", "{invalid-json}")
            );

            assertThat(result.nickname()).isNull();
        }
    }

    // ── client_secret 생성 실패 ────────────────────────────────────────────

    @Nested
    @DisplayName("generateClientSecret() — 키 오류 처리")
    class GenerateClientSecret {

        @Test
        @DisplayName("잘못된 private key이면 BusinessException(INTERNAL_SERVER_ERROR)을 던진다")
        void shouldThrow_whenPrivateKeyIsInvalid() {
            OAuthProperties.Apple badProps = new OAuthProperties.Apple(
                "com.test.app", "TEAM", "KEY",
                "-----BEGIN PRIVATE KEY-----\nbadkey\n-----END PRIVATE KEY-----",
                "https://callback.test",
                15552000000L
            );
            given(oAuthProperties.apple()).willReturn(badProps);
            // oAuthProperties는 mock이므로 badProps 반환으로 덮어씌워짐 →
            // getUserInfo() → exchangeCodeForToken() → generateClientSecret() 호출 시 적용됨

            assertThatThrownBy(() -> appleOAuthClient.getUserInfo(Map.of("code", "code")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR));
        }
    }

    // ── helper ───────────────────────────────────────────────────────────

    /** Apple 토큰 엔드포인트 응답 JSON 생성 */
    private String appleTokenJson(String idToken) {
        return """
            {"access_token":"access","token_type":"bearer","expires_in":3600,
             "refresh_token":"refresh","id_token":"%s"}
            """.formatted(idToken);
    }

    private String buildTestIdToken(String sub, String email) {
        String header = base64Url("{\"alg\":\"RS256\",\"kid\":\"testkey\"}");
        String payload = String.format(
            "{\"iss\":\"https://appleid.apple.com\",\"aud\":\"com.test.app\",\"sub\":\"%s\"%s}",
            sub,
            email != null ? ",\"email\":\"" + email + "\"" : ""
        );
        return header + "." + base64Url(payload) + "." + base64Url("fake-signature");
    }

    private String base64Url(String input) {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }
}
