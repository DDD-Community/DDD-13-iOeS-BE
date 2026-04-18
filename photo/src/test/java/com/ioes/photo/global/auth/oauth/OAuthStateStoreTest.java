package com.ioes.photo.global.auth.oauth;

import com.ioes.photo.global.config.oauth.properties.OAuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * {@link OAuthStateStore} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthStateStore 단위 테스트")
class OAuthStateStoreTest {

    @Mock OAuthProperties                oAuthProperties;
    @Mock RedisTemplate<String, String> redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks OAuthStateStore stateStore;

    @BeforeEach
    void setUp() {
        OAuthProperties.State stateProps = new OAuthProperties.State("oauth_state:", 300L);
        given(oAuthProperties.state()).willReturn(stateProps);
        given(redisTemplate.opsForValue()).willReturn(valueOps);
    }

    // ── save ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("state를 키로 code_verifier를 5분 TTL로 Redis에 저장한다")
        void shouldSaveWithCorrectKeyAndTtl() {
            stateStore.save("test-state", "code-verifier-value");

            then(valueOps).should().set(
                eq("oauth_state:test-state"),
                eq("code-verifier-value"),
                eq(300L),
                eq(TimeUnit.SECONDS)
            );
        }

        @Test
        @DisplayName("키는 oauth_state: 접두사를 포함한다")
        void shouldUsePrefixedKey() {
            stateStore.save("abc123", "verifier");

            then(valueOps).should().set(
                eq("oauth_state:abc123"),
                anyString(),
                anyLong(),
                eq(TimeUnit.SECONDS)
            );
        }
    }

    // ── getAndDelete ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAndDelete()")
    class GetAndDelete {

        @Test
        @DisplayName("저장된 state이면 code_verifier를 반환하고 Redis에서 삭제한다")
        void shouldReturnVerifierAndDelete_whenStateExists() {
            given(valueOps.get("oauth_state:valid-state")).willReturn("my-verifier");

            String result = stateStore.getAndDelete("valid-state");

            assertThat(result).isEqualTo("my-verifier");
            then(redisTemplate).should().delete("oauth_state:valid-state");
        }

        @Test
        @DisplayName("존재하지 않는 state이면 null을 반환하고 삭제를 시도하지 않는다")
        void shouldReturnNull_whenStateNotFound() {
            given(valueOps.get("oauth_state:unknown")).willReturn(null);

            String result = stateStore.getAndDelete("unknown");

            assertThat(result).isNull();
            then(redisTemplate).should(org.mockito.Mockito.never()).delete(anyString());
        }

        @Test
        @DisplayName("일회성 검증 — 동일 state로 두 번 조회하면 두 번째는 null이어야 한다")
        void shouldBeOneTimeUse_conceptually() {
            given(valueOps.get("oauth_state:one-time")).willReturn("verifier").willReturn(null);

            String first  = stateStore.getAndDelete("one-time");
            String second = stateStore.getAndDelete("one-time");

            assertThat(first).isEqualTo("verifier");
            assertThat(second).isNull();
        }
    }
}
