package com.ioes.photo.global.storage;

import com.ioes.photo.global.common.util.NullUtils;
import com.ioes.photo.global.config.image.ImageProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * PRIVATE 이미지의 Presigned URL을 사용자별로 Redis에 보관한다.
 *
 * Presigned URL은 서명 시각이 URL에 들어가 호출마다 문자열이 바뀌고, 앱의 이미지 캐시는 URL 문자열을
 * 키로 쓰므로 같은 사진을 매번 새로 받는다. TTL 안에서는 같은 URL을 내려 이 재다운로드를 막는다.
 *
 * 키는 presigned:{userId}:{imageKey}이다. PRIVATE 이미지는 본인만 볼 수 있으므로 사용자별로 나눠,
 * 권한 검사에 실수가 있어도 캐시를 통해 다른 사용자의 URL이 나가지 않게 한다.
 * Redis 장애는 이미지 조회 실패로 이어지면 안 되므로 조회와 저장 오류는 삼키고 새로 서명한다.
 * 오류가 나면 일정 시간 Redis를 건너뛴다. 건너뛰지 않으면 이미지 한 장마다 타임아웃만큼 응답이 늦어진다.
 *
 * @author 황제연
 */
@Slf4j
@Component
public class PresignedUrlCache {

    private static final String KEY_PREFIX = "presigned:";
    private static final String KEY_SEPARATOR = ":";
    private static final String CACHE_METRIC = "presigned_url_cache";
    private static final String CACHE_METRIC_DESCRIPTION = "Presigned URL 캐시 조회 결과";
    private static final String SIGN_METRIC = "presigned_url_sign";
    private static final String SIGN_METRIC_DESCRIPTION = "Presigned URL 서명에 걸린 시간";
    private static final String TAG_RESULT = "result";
    private static final String RESULT_HIT = "hit";
    private static final String RESULT_MISS = "miss";
    private static final String RESULT_ERROR = "error";
    private static final Duration BYPASS_AFTER_ERROR = Duration.ofSeconds(10);

    private final RedisTemplate<String, String> redisTemplate;
    private final ImageProperties imageProperties;
    private final Counter hitCounter;
    private final Counter missCounter;
    private final Counter errorCounter;
    private final Timer signTimer;
    private volatile Instant bypassUntil = Instant.MIN;

    public PresignedUrlCache(RedisTemplate<String, String> redisTemplate, ImageProperties imageProperties,
                             MeterRegistry registry) {
        this.redisTemplate = redisTemplate;
        this.imageProperties = imageProperties;
        this.hitCounter = registerResultCounter(registry, RESULT_HIT);
        this.missCounter = registerResultCounter(registry, RESULT_MISS);
        this.errorCounter = registerResultCounter(registry, RESULT_ERROR);
        this.signTimer = Timer.builder(SIGN_METRIC)
            .description(SIGN_METRIC_DESCRIPTION).register(registry);
    }

    public String getOrSign(Long userId, String imageKey, Supplier<String> signer) {
        if (isBypassed()) {
            return signTimer.record(signer);
        }
        String cacheKey = buildCacheKey(userId, imageKey);
        String cached = findCachedUrl(cacheKey);
        if (NullUtils.isNotNull(cached)) {
            hitCounter.increment();
            return cached;
        }
        String url = signTimer.record(signer);
        if (!isBypassed()) {
            saveUrl(cacheKey, url);
        }
        return url;
    }

    private boolean isBypassed() {
        return Instant.now().isBefore(bypassUntil);
    }

    private void bypassTemporarily() {
        bypassUntil = Instant.now().plus(BYPASS_AFTER_ERROR);
    }

    private String findCachedUrl(String cacheKey) {
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (NullUtils.isNull(cached)) {
                missCounter.increment();
            }
            return cached;
        } catch (DataAccessException e) {
            errorCounter.increment();
            bypassTemporarily();
            log.warn("Presigned URL 캐시 조회 실패, 새로 서명합니다. error={}", e.getMessage());
            return null;
        }
    }

    private void saveUrl(String cacheKey, String url) {
        try {
            redisTemplate.opsForValue().set(cacheKey, url, imageProperties.presignedUrlCacheTtl());
        } catch (DataAccessException e) {
            errorCounter.increment();
            bypassTemporarily();
            log.warn("Presigned URL 캐시 저장 실패. error={}", e.getMessage());
        }
    }

    private Counter registerResultCounter(MeterRegistry registry, String result) {
        return Counter.builder(CACHE_METRIC).tag(TAG_RESULT, result)
            .description(CACHE_METRIC_DESCRIPTION).register(registry);
    }

    private String buildCacheKey(Long userId, String imageKey) {
        return KEY_PREFIX + userId + KEY_SEPARATOR + imageKey;
    }
}
