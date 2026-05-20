package com.ioes.photo.global.auth.oauth.apple;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ioes.photo.global.error.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * {@link ApplePublicKeyProvider} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("ApplePublicKeyProvider 단위 테스트")
class ApplePublicKeyProviderTest {

    private static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";

    private MockRestServiceServer  server;
    private ApplePublicKeyProvider provider;
    private KeyPair                rsaKeyPair;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        rsaKeyPair = kpg.generateKeyPair();

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new ApplePublicKeyProvider(builder.build(), new ObjectMapper());
    }

    @Nested
    @DisplayName("getPublicKey()")
    class GetPublicKey {

        @Test
        @DisplayName("kid가 일치하는 JWK에서 RSA 공개키를 반환한다")
        void shouldReturnPublicKey_whenKidMatches() {
            server.expect(requestTo(APPLE_KEYS_URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(buildJwksJson("target-kid"), MediaType.APPLICATION_JSON));

            PublicKey publicKey = provider.getPublicKey("target-kid");

            assertThat(publicKey).isNotNull();
            assertThat(publicKey.getAlgorithm()).isEqualTo("RSA");
            assertThat(publicKey).isInstanceOf(RSAPublicKey.class);
        }

        @Test
        @DisplayName("RSA 공개키의 모듈러스와 지수가 원본과 일치한다")
        void shouldReturnCorrectRsaKeyComponents() {
            server.expect(requestTo(APPLE_KEYS_URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(buildJwksJson("check-kid"), MediaType.APPLICATION_JSON));

            RSAPublicKey result = (RSAPublicKey) provider.getPublicKey("check-kid");
            RSAPublicKey original = (RSAPublicKey) rsaKeyPair.getPublic();

            assertThat(result.getModulus()).isEqualTo(original.getModulus());
            assertThat(result.getPublicExponent()).isEqualTo(original.getPublicExponent());
        }

        @Test
        @DisplayName("kid가 없으면 BusinessException(UNAUTHORIZED)을 던진다")
        void shouldThrow_whenKidNotFound() {
            server.expect(requestTo(APPLE_KEYS_URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(buildJwksJson("other-kid"), MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> provider.getPublicKey("unknown-kid"))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("두 번 호출해도 JWKS는 한 번만 조회한다 (캐시 동작)")
        void shouldCacheJwks() {
            server.expect(requestTo(APPLE_KEYS_URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(buildJwksJson("cached-kid"), MediaType.APPLICATION_JSON));

            provider.getPublicKey("cached-kid");
            provider.getPublicKey("cached-kid");

            // MockRestServiceServer에 등록된 expectation이 1개이므로 2번 호출 시 예외가 발생하지 않아야 함
            server.verify();
        }
    }

    // ── helper ───────────────────────────────────────────────────────────

    private String buildJwksJson(String kid) {
        RSAPublicKey pub = (RSAPublicKey) rsaKeyPair.getPublic();
        String n = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(pub.getModulus().toByteArray());
        String e = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(pub.getPublicExponent().toByteArray());
        return """
            {"keys":[{"kty":"RSA","kid":"%s","use":"sig","alg":"RS256","n":"%s","e":"%s"}]}
            """.formatted(kid, n, e);
    }
}
