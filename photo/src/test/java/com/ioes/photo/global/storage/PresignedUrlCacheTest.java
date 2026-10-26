package com.ioes.photo.global.storage;

import com.ioes.photo.global.config.image.ImageProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 사용자별 Presigned URL 캐시의 키 형식, TTL, 장애 시 동작을 검증한다.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
class PresignedUrlCacheTest {

    private static final Long USER_ID = 7L;
    private static final String IMAGE_KEY = "prod/private/users/7/archive/202610/a.jpg";
    private static final String EXPECTED_CACHE_KEY = "presigned:7:" + IMAGE_KEY;
    private static final String SIGNED_URL = "https://bucket.s3.amazonaws.com/signed";
    private static final int EXPIRY_MINUTES = 120;
    private static final Duration EXPECTED_TTL = Duration.ofMinutes(60);
    private static final String METRIC_CACHE = "presigned_url_cache";
    private static final String TAG_RESULT = "result";
    private static final String RESULT_HIT = "hit";
    private static final String RESULT_MISS = "miss";
    private static final String RESULT_ERROR = "error";
    private static final String CACHED_URL = "cached-url";
    private static final Long OTHER_USER_ID = 8L;
    private static final int THUMBNAIL_SIZE = 300;

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private SimpleMeterRegistry registry;
    private PresignedUrlCache cache;
    private AtomicInteger signCount;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        ImageProperties properties = new ImageProperties(
            new ImageProperties.ThumbnailProperties(THUMBNAIL_SIZE, THUMBNAIL_SIZE), EXPIRY_MINUTES,
            new ImageProperties.ResizeProperties(4, 30, 0.5, 0.4));
        registry = new SimpleMeterRegistry();
        cache = new PresignedUrlCache(redisTemplate, properties, registry);
        signCount = new AtomicInteger();
    }

    private String sign() {
        signCount.incrementAndGet();
        return SIGNED_URL;
    }

    private double count(String result) {
        return registry.counter(METRIC_CACHE, TAG_RESULT, result).count();
    }

    @Test
    @DisplayName("캐시에 있으면 서명하지 않고 그 URL을 돌려준다")
    void returnsCachedUrlWithoutSigning() {
        given(valueOperations.get(EXPECTED_CACHE_KEY)).willReturn(CACHED_URL);

        String url = cache.getOrSign(USER_ID, IMAGE_KEY, this::sign);

        assertThat(url).isEqualTo(CACHED_URL);
        assertThat(signCount).hasValue(0);
        assertThat(count(RESULT_HIT)).isEqualTo(1);
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("캐시에 없으면 서명한 뒤 사용자별 키와 만료의 절반 TTL로 저장한다")
    void signsAndStoresOnMiss() {
        given(valueOperations.get(EXPECTED_CACHE_KEY)).willReturn(null);

        String url = cache.getOrSign(USER_ID, IMAGE_KEY, this::sign);

        assertThat(url).isEqualTo(SIGNED_URL);
        assertThat(signCount).hasValue(1);
        assertThat(count(RESULT_MISS)).isEqualTo(1);
        verify(valueOperations).set(EXPECTED_CACHE_KEY, SIGNED_URL, EXPECTED_TTL);
    }

    @Test
    @DisplayName("다른 사용자는 같은 이미지 경로여도 다른 캐시 키를 쓴다")
    void usesSeparateKeyPerUser() {
        given(valueOperations.get(anyString())).willReturn(null);

        cache.getOrSign(OTHER_USER_ID, IMAGE_KEY, this::sign);

        verify(valueOperations).get("presigned:" + OTHER_USER_ID + ":" + IMAGE_KEY);
        verify(valueOperations, never()).get(EXPECTED_CACHE_KEY);
    }

    @Test
    @DisplayName("Redis 조회가 실패해도 새로 서명해서 돌려준다")
    void signsWhenLookupFails() {
        given(valueOperations.get(anyString())).willThrow(new RedisConnectionFailureException("down"));

        String url = cache.getOrSign(USER_ID, IMAGE_KEY, this::sign);

        assertThat(url).isEqualTo(SIGNED_URL);
        assertThat(signCount).hasValue(1);
        assertThat(count(RESULT_ERROR)).isEqualTo(1);
    }

    @Test
    @DisplayName("Redis 오류 직후에는 Redis를 건너뛰고 바로 서명한다")
    void bypassesRedisRightAfterError() {
        given(valueOperations.get(anyString())).willThrow(new RedisConnectionFailureException("down"));

        cache.getOrSign(USER_ID, IMAGE_KEY, this::sign);
        cache.getOrSign(USER_ID, IMAGE_KEY, this::sign);

        assertThat(signCount).hasValue(2);
        assertThat(count(RESULT_ERROR)).isEqualTo(1);
        verify(valueOperations, times(1)).get(anyString());
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Redis 저장이 실패해도 서명한 URL을 돌려준다")
    void returnsUrlWhenStoreFails() {
        given(valueOperations.get(anyString())).willReturn(null);
        willThrow(new RedisConnectionFailureException("down"))
            .given(valueOperations).set(eq(EXPECTED_CACHE_KEY), anyString(), any(Duration.class));

        String url = cache.getOrSign(USER_ID, IMAGE_KEY, this::sign);

        assertThat(url).isEqualTo(SIGNED_URL);
        assertThat(count(RESULT_ERROR)).isEqualTo(1);
    }
}
