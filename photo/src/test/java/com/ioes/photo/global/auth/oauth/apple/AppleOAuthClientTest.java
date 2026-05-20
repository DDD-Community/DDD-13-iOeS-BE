package com.ioes.photo.global.auth.oauth.apple;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ioes.photo.global.auth.oauth.OAuthProvider;
import com.ioes.photo.global.auth.oauth.OAuthUserInfo;
import com.ioes.photo.global.config.oauth.properties.OAuthProperties;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import io.jsonwebtoken.Jwts;
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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * {@link AppleOAuthClient} 단위 테스트.
 *
 * 네이티브 SDK 플로우: 앱이 Apple SDK로 받은 identityToken(RS256 JWT)을
 * 백엔드가 Apple JWKS를 통해 검증합니다.
 * ApplePublicKeyProvider는 Mock으로 처리합니다.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AppleOAuthClient 단위 테스트")
class AppleOAuthClientTest {

    private static final String APPLE_REVOKE_URL = "https://appleid.apple.com/auth/revoke";
    private static final String APPLE_CLIENT_ID  = "com.test.app";
    private static final String APPLE_ISSUER     = "https://appleid.apple.com";
    private static final String TEST_KID         = "testkey";

    @Mock OAuthProperties           oAuthProperties;
    @Mock ApplePublicKeyProvider    applePublicKeyProvider;

    private final ObjectMapper    objectMapper = new ObjectMapper();
    private MockRestServiceServer server;
    private AppleOAuthClient      appleOAuthClient;
    private OAuthProperties.Apple appleProps;

    // RSA 키쌍 (identity token 서명/검증용)
    private KeyPair rsaKeyPair;
    // EC 키쌍 (client_secret JWT 서명용)
    private KeyPair ecKeyPair;
    private String  validPrivateKeyPem;

    @BeforeEach
    void setUp() throws Exception {
        // RSA 키쌍 생성 (Apple identity token 서명)
        KeyPairGenerator rsaKpg = KeyPairGenerator.getInstance("RSA");
        rsaKpg.initialize(2048);
        rsaKeyPair = rsaKpg.generateKeyPair();

        // EC 키쌍 생성 (Apple client_secret JWT 서명)
        KeyPairGenerator ecKpg = KeyPairGenerator.getInstance("EC");
        ecKpg.initialize(new ECGenParameterSpec("secp256r1"));
        ecKeyPair = ecKpg.generateKeyPair();
        validPrivateKeyPem = "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getEncoder().encodeToString(ecKeyPair.getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----";

        appleProps = new OAuthProperties.Apple(
            APPLE_CLIENT_ID, "TEST1TEAM1", "TESTKEY123",
            validPrivateKeyPem,
            15552000000L
        );
        given(oAuthProperties.apple()).willReturn(appleProps);

        // 기본적으로 kid=TEST_KID에 대해 RSA 공개키 반환
        given(applePublicKeyProvider.getPublicKey(TEST_KID)).willReturn(rsaKeyPair.getPublic());

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        appleOAuthClient = new AppleOAuthClient(builder.build(), objectMapper, oAuthProperties, applePublicKeyProvider);
    }

    // ── getProvider ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getProvider()는 APPLE을 반환한다")
    void shouldReturnAppleProvider() {
        assertThat(appleOAuthClient.getProvider()).isEqualTo(OAuthProvider.APPLE);
    }

    // ── getUserInfo ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserInfo() — identity token 검증")
    class GetUserInfo {

        @Test
        @DisplayName("유효한 identity token에서 sub와 email을 추출한다")
        void shouldExtractSubAndEmail() {
            String identityToken = buildSignedIdToken("apple-user-sub-001", "apple@test.com");

            OAuthUserInfo result = appleOAuthClient.getUserInfo(
                Map.of("identityToken", identityToken)
            );

            assertThat(result.provider()).isEqualTo(OAuthProvider.APPLE);
            assertThat(result.providerId()).isEqualTo("apple-user-sub-001");
            assertThat(result.email()).isEqualTo("apple@test.com");
            assertThat(result.profileImageUrl()).isNull();
            assertThat(result.providerRefreshToken()).isNull();
        }

        @Test
        @DisplayName("email이 없는 identity token도 처리한다")
        void shouldHandleTokenWithoutEmail() {
            String identityToken = buildSignedIdToken("sub-no-email", null);

            OAuthUserInfo result = appleOAuthClient.getUserInfo(
                Map.of("identityToken", identityToken)
            );

            assertThat(result.providerId()).isEqualTo("sub-no-email");
            assertThat(result.email()).isNull();
        }

        @Test
        @DisplayName("최초 로그인 시 nickname이 params에 포함되면 그대로 반환한다")
        void shouldIncludeNickname_whenPresentInParams() {
            String identityToken = buildSignedIdToken("sub-001", "test@apple.com");

            OAuthUserInfo result = appleOAuthClient.getUserInfo(
                Map.of("identityToken", identityToken, "nickname", "John Doe")
            );

            assertThat(result.nickname()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("재로그인 시 nickname params 없으면 nickname이 null이다")
        void shouldReturnNullNickname_whenAbsent() {
            String identityToken = buildSignedIdToken("sub-001", "test@apple.com");

            OAuthUserInfo result = appleOAuthClient.getUserInfo(
                Map.of("identityToken", identityToken)
            );

            assertThat(result.nickname()).isNull();
        }

        @Test
        @DisplayName("JWT 형식이 유효하지 않으면 BusinessException(UNAUTHORIZED)을 던진다")
        void shouldThrow_whenInvalidJwtFormat() {
            assertThatThrownBy(() -> appleOAuthClient.getUserInfo(Map.of("identityToken", "not-a-jwt")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(CommonErrorCode.UNAUTHORIZED));
        }

        @Test
        @DisplayName("audience가 clientId와 다르면 BusinessException(UNAUTHORIZED)을 던진다")
        void shouldThrow_whenAudienceMismatch() {
            // audience가 다른 앱 ID인 토큰
            String wrongAudToken = Jwts.builder()
                .header().add("kid", TEST_KID).and()
                .issuer(APPLE_ISSUER)
                .audience().add("com.other.app").and()
                .subject("some-sub")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(rsaKeyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

            assertThatThrownBy(() -> appleOAuthClient.getUserInfo(Map.of("identityToken", wrongAudToken)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(CommonErrorCode.UNAUTHORIZED));
        }

        @Test
        @DisplayName("만료된 identity token이면 BusinessException(UNAUTHORIZED)을 던진다")
        void shouldThrow_whenTokenExpired() {
            String expiredToken = Jwts.builder()
                .header().add("kid", TEST_KID).and()
                .issuer(APPLE_ISSUER)
                .audience().add(APPLE_CLIENT_ID).and()
                .subject("sub-expired")
                .issuedAt(new Date(System.currentTimeMillis() - 7200_000))
                .expiration(new Date(System.currentTimeMillis() - 3600_000))
                .signWith(rsaKeyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

            assertThatThrownBy(() -> appleOAuthClient.getUserInfo(Map.of("identityToken", expiredToken)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(CommonErrorCode.UNAUTHORIZED));
        }

        @Test
        @DisplayName("잘못된 서명이면 BusinessException(UNAUTHORIZED)을 던진다")
        void shouldThrow_whenSignatureInvalid() throws Exception {
            // 다른 RSA 키로 서명
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair otherKey = kpg.generateKeyPair();

            String tamperedToken = Jwts.builder()
                .header().add("kid", TEST_KID).and()
                .issuer(APPLE_ISSUER)
                .audience().add(APPLE_CLIENT_ID).and()
                .subject("sub-tampered")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(otherKey.getPrivate(), Jwts.SIG.RS256)
                .compact();

            // 검증에 사용하는 공개키는 원래 rsaKeyPair.getPublic() → 서명 불일치
            assertThatThrownBy(() -> appleOAuthClient.getUserInfo(Map.of("identityToken", tamperedToken)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(CommonErrorCode.UNAUTHORIZED));
        }
    }

    // ── revokeConnection ──────────────────────────────────────────────────

    @Nested
    @DisplayName("revokeConnection()")
    class RevokeConnection {

        @Test
        @DisplayName("refresh_token이 있으면 Apple revoke API를 호출한다")
        void shouldCallRevokeApi_whenRefreshTokenPresent() {
            server.expect(requestTo(APPLE_REVOKE_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

            appleOAuthClient.revokeConnection("apple-sub-123", "valid-refresh-token");

            server.verify();
        }

        @Test
        @DisplayName("refresh_token이 null이면 API 호출 없이 경고 로그만 남긴다")
        void shouldSkip_whenRefreshTokenNull() {
            appleOAuthClient.revokeConnection("apple-sub-123", null);
        }

        @Test
        @DisplayName("refresh_token이 빈 문자열이면 API 호출 없이 경고 로그만 남긴다")
        void shouldSkip_whenRefreshTokenBlank() {
            appleOAuthClient.revokeConnection("apple-sub-123", "");
        }
    }

    // ── client_secret 생성 실패 ────────────────────────────────────────────

    @Nested
    @DisplayName("revokeConnection() — client_secret 생성 오류")
    class ClientSecretError {

        @Test
        @DisplayName("잘못된 private key이면 BusinessException(INTERNAL_SERVER_ERROR)을 던진다")
        void shouldThrow_whenPrivateKeyIsInvalid() {
            OAuthProperties.Apple badProps = new OAuthProperties.Apple(
                APPLE_CLIENT_ID, "TEAM", "KEY",
                "-----BEGIN PRIVATE KEY-----\nbadkey\n-----END PRIVATE KEY-----",
                15552000000L
            );
            given(oAuthProperties.apple()).willReturn(badProps);

            assertThatThrownBy(() -> appleOAuthClient.revokeConnection("sub", "some-refresh-token"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR));
        }
    }

    // ── helper ───────────────────────────────────────────────────────────

    private String buildSignedIdToken(String sub, String email) {
        Date now = new Date();
        var builder = Jwts.builder()
            .header().add("kid", TEST_KID).and()
            .issuer(APPLE_ISSUER)
            .audience().add(APPLE_CLIENT_ID).and()
            .subject(sub)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + 3600_000))
            .signWith(rsaKeyPair.getPrivate(), Jwts.SIG.RS256);
        if (email != null) {
            builder.claim("email", email);
        }
        return builder.compact();
    }
}
