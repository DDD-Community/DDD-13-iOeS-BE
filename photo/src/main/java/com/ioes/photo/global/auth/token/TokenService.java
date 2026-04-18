package com.ioes.photo.global.auth.token;

import com.ioes.photo.global.config.security.JwtProvider;
import com.ioes.photo.global.config.security.properties.TokenProperties;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Access Token / Refresh Token 발급 및 관리 서비스
 * Refresh Token은 Redis에 저장되며, 토큰 갱신 시 기존 토큰을 무효화하고 새 토큰을 발급합니다 (Refresh Token Rotation)
 *
 *
 * @author 황제연
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final TokenProperties tokenProperties;
    public String[] issueTokens(String userId) {
        String accessToken  = jwtProvider.generateToken(userId);
        String refreshToken = generateRefreshToken();
        storeRefreshToken(refreshToken, userId);
        return new String[]{accessToken, refreshToken};
    }

    public String[] refreshTokens(String refreshToken) {
        String key = tokenProperties.refreshKeyPrefix() + refreshToken;
        String userId = redisTemplate.opsForValue().get(key);

        if (userId == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED,
                    "유효하지 않거나 만료된 Refresh Token입니다");
        }

        redisTemplate.delete(key); // 기존 토큰 즉시 무효화 (Rotation)
        return issueTokens(userId);
    }

    public void invalidateRefreshToken(String refreshToken) {
        String key = tokenProperties.refreshKeyPrefix() + refreshToken;
        String userId = redisTemplate.opsForValue().get(key);
        Boolean deleted = redisTemplate.delete(key);
        if (Boolean.TRUE.equals(deleted) && userId != null) {
            redisTemplate.opsForSet().remove(tokenProperties.userTokensKeyPrefix() + userId, refreshToken);
        }
        log.debug("Refresh Token 무효화: {}",
                Boolean.TRUE.equals(deleted) ? "성공"
                : "이미 만료됨");
    }

    public void invalidateAllUserTokens(String userId) {
        String userTokensKey = tokenProperties.userTokensKeyPrefix() + userId;
        Set<String> tokens   = redisTemplate.opsForSet().members(userTokensKey);
        if (tokens != null && !tokens.isEmpty()) {
            for (String token : tokens) {
                redisTemplate.delete(tokenProperties.refreshKeyPrefix() + token);
            }
        }
        redisTemplate.delete(userTokensKey);
        redisTemplate.delete(tokenProperties.providerRtKeyPrefix() + userId);
        log.info("사용자 {} 의 모든 Refresh Token 무효화 완료 ({}개)", userId,
            tokens != null ? tokens.size() : 0);
    }

    public void storeProviderRefreshToken(String userId, String providerRefreshToken) {
        redisTemplate.opsForValue().set(
            tokenProperties.providerRtKeyPrefix() + userId,
            providerRefreshToken
        );
    }

    public String getProviderRefreshToken(String userId) {
        return redisTemplate.opsForValue().get(tokenProperties.providerRtKeyPrefix() + userId);
    }

    public boolean isRefreshTokenValid(String refreshToken) {
        return Boolean.TRUE.equals(
            redisTemplate.hasKey(tokenProperties.refreshKeyPrefix() + refreshToken)
        );
    }

    private String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void storeRefreshToken(String refreshToken, String userId) {
        long expirationMs = tokenProperties.refreshExpirationMs();
        redisTemplate.opsForValue().set(
            tokenProperties.refreshKeyPrefix() + refreshToken,
            userId,
            expirationMs,
            TimeUnit.MILLISECONDS
        );
        // 역방향 추적: 회원탈퇴 시 모든 토큰 일괄 삭제에 사용
        String userTokensKey = tokenProperties.userTokensKeyPrefix() + userId;
        redisTemplate.opsForSet().add(userTokensKey, refreshToken);
        redisTemplate.expire(userTokensKey, expirationMs, TimeUnit.MILLISECONDS);
    }
}
