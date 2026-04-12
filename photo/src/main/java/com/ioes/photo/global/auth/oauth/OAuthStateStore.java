package com.ioes.photo.global.auth.oauth;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * OAuth State / PKCE code_verifier를 Redis에 임시 저장하는 저장소.
 *
 * State는 CSRF 방지, code_verifier는 PKCE(Proof Key for Code Exchange)를 위해 사용됩니다.
 * 인증 URL 생성 시 저장하고, 콜백 수신 시 검증 후 즉시 삭제(일회성)합니다.
 *
 * @author 황제연
 */
@Component
@RequiredArgsConstructor
public class OAuthStateStore {

    private static final String KEY_PREFIX  = "oauth_state:";
    private static final long   TTL_SECONDS = 300; // 5분

    private final RedisTemplate<String, String> redisTemplate;
    public void save(String state, String codeVerifier) {
        redisTemplate.opsForValue().set(KEY_PREFIX + state, codeVerifier, TTL_SECONDS, TimeUnit.SECONDS);
    }

    public String getAndDelete(String state) {
        String key = KEY_PREFIX + state;
        String codeVerifier = redisTemplate.opsForValue().get(key);
        if (codeVerifier != null) {
            redisTemplate.delete(key);
        }
        return codeVerifier;
    }
}