package com.ioes.photo.global.auth.token;

import com.ioes.photo.global.config.security.JwtProvider;
import com.ioes.photo.global.config.security.properties.TokenProperties;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * {@link TokenService} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TokenService 단위 테스트")
class TokenServiceTest {

    @Mock JwtProvider jwtProvider;
    @Mock RedisTemplate<String, String> redisTemplate;
    @Mock ValueOperations<String, String> valueOperations;
    @Mock SetOperations<String, String> setOperations;
    @Mock TokenProperties tokenProperties;

    @InjectMocks TokenService tokenService;

    private static final String TEST_USER_ID    = "1";
    private static final String TEST_ACCESS_TOKEN = "test.access.token";
    private static final long   REFRESH_TTL_MS  = 86400000L; // 1일
    private static final String REFRESH_PREFIX  = "refresh:";
    private static final String USER_TOKENS_PREFIX = "user_tokens:";

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(tokenProperties.refreshExpirationMs()).willReturn(REFRESH_TTL_MS);
        given(tokenProperties.refreshKeyPrefix()).willReturn(REFRESH_PREFIX);
        given(tokenProperties.userTokensKeyPrefix()).willReturn(USER_TOKENS_PREFIX);
        given(jwtProvider.generateToken(TEST_USER_ID)).willReturn(TEST_ACCESS_TOKEN);
    }

    // ── issueTokens ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("issueTokens()")
    class IssueTokens {

        @Test
        @DisplayName("Access Token과 Refresh Token 쌍을 반환한다")
        void shouldReturnTokenPair() {
            String[] tokens = tokenService.issueTokens(TEST_USER_ID);

            assertThat(tokens).hasSize(2);
            assertThat(tokens[0]).isEqualTo(TEST_ACCESS_TOKEN);
            assertThat(tokens[1]).isNotBlank();
        }

        @Test
        @DisplayName("Refresh Token을 Redis에 TTL과 함께 저장한다")
        void shouldStoreRefreshTokenInRedis() {
            String[] tokens = tokenService.issueTokens(TEST_USER_ID);
            String refreshToken = tokens[1];

            then(valueOperations).should().set(
                eq(REFRESH_PREFIX + refreshToken),
                eq(TEST_USER_ID),
                eq(REFRESH_TTL_MS),
                eq(TimeUnit.MILLISECONDS)
            );
        }

        @Test
        @DisplayName("Refresh Token을 유저별 Set에도 추적 저장한다")
        void shouldTrackRefreshTokenInUserSet() {
            String[] tokens = tokenService.issueTokens(TEST_USER_ID);
            String refreshToken = tokens[1];

            then(setOperations).should().add(
                eq(USER_TOKENS_PREFIX + TEST_USER_ID),
                eq(refreshToken)
            );
            then(redisTemplate).should().expire(
                eq(USER_TOKENS_PREFIX + TEST_USER_ID),
                eq(REFRESH_TTL_MS),
                eq(TimeUnit.MILLISECONDS)
            );
        }

        @Test
        @DisplayName("Refresh Token은 32자리 UUID hex 문자열이다")
        void refreshTokenShouldBeUuidHex() {
            String[] tokens = tokenService.issueTokens(TEST_USER_ID);

            assertThat(tokens[1])
                .hasSize(32)
                .matches("[0-9a-f]{32}");
        }
    }

    // ── refreshTokens ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("refreshTokens()")
    class RefreshTokens {

        @Test
        @DisplayName("유효한 Refresh Token으로 새 토큰 쌍을 반환한다")
        void shouldReturnNewTokenPair_whenValidToken() {
            String oldRefreshToken = "abc123";
            given(valueOperations.get(REFRESH_PREFIX + oldRefreshToken)).willReturn(TEST_USER_ID);

            String[] newTokens = tokenService.refreshTokens(oldRefreshToken);

            assertThat(newTokens).hasSize(2);
            assertThat(newTokens[0]).isEqualTo(TEST_ACCESS_TOKEN);
        }

        @Test
        @DisplayName("유효한 Refresh Token 사용 시 기존 토큰을 즉시 삭제한다 (Rotation)")
        void shouldDeleteOldToken_whenRefreshing() {
            String oldRefreshToken = "abc123";
            given(valueOperations.get(REFRESH_PREFIX + oldRefreshToken)).willReturn(TEST_USER_ID);

            tokenService.refreshTokens(oldRefreshToken);

            then(redisTemplate).should().delete(REFRESH_PREFIX + oldRefreshToken);
        }

        @Test
        @DisplayName("Redis에 없는 Refresh Token이면 BusinessException을 던진다")
        void shouldThrow_whenTokenNotInRedis() {
            given(valueOperations.get(anyString())).willReturn(null);

            assertThatThrownBy(() -> tokenService.refreshTokens("invalid-token"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Refresh Token");
        }

        @Test
        @DisplayName("유효하지 않은 토큰으로 갱신 시도 시 Redis 저장을 하지 않는다")
        void shouldNotIssueNewToken_whenInvalidToken() {
            given(valueOperations.get(anyString())).willReturn(null);

            assertThatThrownBy(() -> tokenService.refreshTokens("invalid"))
                .isInstanceOf(BusinessException.class);

            then(valueOperations).should(never()).set(any(), any(), anyLong(), any());
        }
    }

    // ── invalidateRefreshToken ────────────────────────────────────────────

    @Nested
    @DisplayName("invalidateRefreshToken()")
    class InvalidateRefreshToken {

        @Test
        @DisplayName("Refresh Token을 Redis에서 삭제한다")
        void shouldDeleteFromRedis() {
            String refreshToken = "token-to-delete";
            given(valueOperations.get(REFRESH_PREFIX + refreshToken)).willReturn(TEST_USER_ID);
            given(redisTemplate.delete(REFRESH_PREFIX + refreshToken)).willReturn(true);

            tokenService.invalidateRefreshToken(refreshToken);

            then(redisTemplate).should().delete(REFRESH_PREFIX + refreshToken);
        }

        @Test
        @DisplayName("삭제 성공 시 유저 Set에서도 토큰을 제거한다")
        void shouldRemoveFromUserSet_whenDeleted() {
            String refreshToken = "token-to-delete";
            given(valueOperations.get(REFRESH_PREFIX + refreshToken)).willReturn(TEST_USER_ID);
            given(redisTemplate.delete(REFRESH_PREFIX + refreshToken)).willReturn(true);

            tokenService.invalidateRefreshToken(refreshToken);

            then(setOperations).should().remove(
                eq(USER_TOKENS_PREFIX + TEST_USER_ID),
                eq(refreshToken)
            );
        }

        @Test
        @DisplayName("이미 만료된 토큰이면 유저 Set에서 제거하지 않는다")
        void shouldNotRemoveFromUserSet_whenAlreadyExpired() {
            String refreshToken = "already-expired";
            given(valueOperations.get(REFRESH_PREFIX + refreshToken)).willReturn(null);
            given(redisTemplate.delete(REFRESH_PREFIX + refreshToken)).willReturn(false);

            tokenService.invalidateRefreshToken(refreshToken);

            then(setOperations).should(never()).remove(any(), any());
        }
    }

    // ── invalidateAllUserTokens ───────────────────────────────────────────

    @Nested
    @DisplayName("invalidateAllUserTokens()")
    class InvalidateAllUserTokens {

        @Test
        @DisplayName("유저의 모든 Refresh Token을 Redis에서 삭제한다")
        void shouldDeleteAllUserTokens() {
            String token1 = "token-aaa";
            String token2 = "token-bbb";
            given(setOperations.members(USER_TOKENS_PREFIX + TEST_USER_ID))
                .willReturn(Set.of(token1, token2));

            tokenService.invalidateAllUserTokens(TEST_USER_ID);

            then(redisTemplate).should().delete(REFRESH_PREFIX + token1);
            then(redisTemplate).should().delete(REFRESH_PREFIX + token2);
        }

        @Test
        @DisplayName("유저 Set 자체도 삭제한다")
        void shouldDeleteUserTokenSet() {
            given(setOperations.members(USER_TOKENS_PREFIX + TEST_USER_ID))
                .willReturn(Set.of("some-token"));

            tokenService.invalidateAllUserTokens(TEST_USER_ID);

            then(redisTemplate).should().delete(USER_TOKENS_PREFIX + TEST_USER_ID);
        }

        @Test
        @DisplayName("발급된 토큰이 없어도 예외 없이 처리한다")
        void shouldHandleEmptyTokenSet() {
            given(setOperations.members(USER_TOKENS_PREFIX + TEST_USER_ID))
                .willReturn(Set.of());

            tokenService.invalidateAllUserTokens(TEST_USER_ID);

            then(redisTemplate).should().delete(USER_TOKENS_PREFIX + TEST_USER_ID);
            then(redisTemplate).should(never()).delete(startsWith(REFRESH_PREFIX));
        }

        @Test
        @DisplayName("Redis Set이 null을 반환해도 예외 없이 처리한다")
        void shouldHandleNullTokenSet() {
            given(setOperations.members(anyString())).willReturn(null);

            tokenService.invalidateAllUserTokens(TEST_USER_ID);

            then(redisTemplate).should().delete(USER_TOKENS_PREFIX + TEST_USER_ID);
        }
    }

    // ── isRefreshTokenValid ───────────────────────────────────────────────

    @Nested
    @DisplayName("isRefreshTokenValid()")
    class IsRefreshTokenValid {

        @Test
        @DisplayName("Redis에 존재하는 토큰이면 true를 반환한다")
        void shouldReturnTrue_whenExists() {
            given(redisTemplate.hasKey(REFRESH_PREFIX + "valid-token")).willReturn(Boolean.TRUE);

            assertThat(tokenService.isRefreshTokenValid("valid-token")).isTrue();
        }

        @Test
        @DisplayName("Redis에 없는 토큰이면 false를 반환한다")
        void shouldReturnFalse_whenNotExists() {
            given(redisTemplate.hasKey(anyString())).willReturn(Boolean.FALSE);

            assertThat(tokenService.isRefreshTokenValid("missing-token")).isFalse();
        }

        @Test
        @DisplayName("Redis가 null을 반환해도 false를 반환한다")
        void shouldReturnFalse_whenRedisReturnsNull() {
            given(redisTemplate.hasKey(anyString())).willReturn(null);

            assertThat(tokenService.isRefreshTokenValid("null-case")).isFalse();
        }
    }

    // ── logout ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("logout()")
    class Logout {

        private static final String BLACKLIST_PREFIX = "blacklist:";
        private static final long REMAINING_MS = 3600000L;

        @BeforeEach
        void setUpBlacklist() {
            given(tokenProperties.blacklistKeyPrefix()).willReturn(BLACKLIST_PREFIX);
        }

        @Test
        @DisplayName("Refresh Token을 무효화하고 Access Token을 블랙리스트에 등록한다")
        void shouldInvalidateRefreshAndBlacklistAccess() {
            String refreshToken = "refresh-to-logout";
            given(valueOperations.get(REFRESH_PREFIX + refreshToken)).willReturn(TEST_USER_ID);
            given(redisTemplate.delete(REFRESH_PREFIX + refreshToken)).willReturn(true);
            given(jwtProvider.getRemainingValidityMs(TEST_ACCESS_TOKEN)).willReturn(REMAINING_MS);

            tokenService.logout(refreshToken, TEST_ACCESS_TOKEN);

            then(redisTemplate).should().delete(REFRESH_PREFIX + refreshToken);
            then(valueOperations).should().set(
                eq(BLACKLIST_PREFIX + TEST_ACCESS_TOKEN),
                eq("blacklisted"),
                eq(REMAINING_MS),
                eq(TimeUnit.MILLISECONDS)
            );
        }

        @Test
        @DisplayName("Access Token이 null이면 블랙리스트 등록 없이 Refresh Token만 무효화한다")
        void shouldOnlyInvalidateRefresh_whenAccessTokenNull() {
            String refreshToken = "refresh-only";
            given(valueOperations.get(REFRESH_PREFIX + refreshToken)).willReturn(TEST_USER_ID);
            given(redisTemplate.delete(REFRESH_PREFIX + refreshToken)).willReturn(true);

            tokenService.logout(refreshToken, null);

            then(redisTemplate).should().delete(REFRESH_PREFIX + refreshToken);
            then(valueOperations).should(never()).set(startsWith(BLACKLIST_PREFIX), any(), anyLong(), any());
        }
    }

    // ── isBlacklisted ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("isBlacklisted()")
    class IsBlacklisted {

        private static final String BLACKLIST_PREFIX = "blacklist:";

        @BeforeEach
        void setUpBlacklist() {
            given(tokenProperties.blacklistKeyPrefix()).willReturn(BLACKLIST_PREFIX);
        }

        @Test
        @DisplayName("블랙리스트에 있는 토큰이면 true를 반환한다")
        void shouldReturnTrue_whenBlacklisted() {
            given(redisTemplate.hasKey(BLACKLIST_PREFIX + TEST_ACCESS_TOKEN)).willReturn(Boolean.TRUE);

            assertThat(tokenService.isBlacklisted(TEST_ACCESS_TOKEN)).isTrue();
        }

        @Test
        @DisplayName("블랙리스트에 없는 토큰이면 false를 반환한다")
        void shouldReturnFalse_whenNotBlacklisted() {
            given(redisTemplate.hasKey(BLACKLIST_PREFIX + TEST_ACCESS_TOKEN)).willReturn(Boolean.FALSE);

            assertThat(tokenService.isBlacklisted(TEST_ACCESS_TOKEN)).isFalse();
        }
    }
}
