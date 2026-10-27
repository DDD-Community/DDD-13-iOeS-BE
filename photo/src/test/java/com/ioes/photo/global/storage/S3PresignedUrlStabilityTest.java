package com.ioes.photo.global.storage;

import com.ioes.photo.global.config.file.properties.FileProperties;
import com.ioes.photo.global.config.image.ImageProperties;
import com.ioes.photo.global.config.s3.properties.S3Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * PRIVATE 이미지의 Presigned URL이 호출마다 달라지는지 실측하는 테스트.
 *
 * PUBLIC은 CloudFront 고정 URL이라 같은 키면 항상 같은 문자열이고,
 * PRIVATE은 서명 시각(X-Amz-Date)과 서명(X-Amz-Signature)이 초 단위로 바뀐다.
 * 클라이언트 이미지 캐시는 URL 문자열을 키로 쓰므로 URL이 바뀌면 같은 사진도 새 이미지로 취급해 다시 받는다.
 *
 * 초 단위 대기가 있어 환경변수 RUN_MEASUREMENT=true 일 때만 돈다.
 * 캐시 쪽 테스트는 실제 Redis 없이 get/set만 흉내 낸 메모리 저장소를 쓴다. TTL 만료는 관심사가 아니다.
 *
 * @author 황제연
 */
@EnabledIfEnvironmentVariable(named = "RUN_MEASUREMENT", matches = "true")
class S3PresignedUrlStabilityTest {

    private static final String PUBLIC_KEY = "prod/public/spots/1/thumbnail/202610/aaa.jpg";
    private static final String PRIVATE_KEY = "prod/private/spots/1/thumbnail/202610/aaa.jpg";
    private static final int THUMBNAIL_SIZE = 300;
    private static final int EXPIRY_MINUTES = 60;
    private static final Long USER_ID = 7L;
    private static final Long OTHER_USER_ID = 8L;
    private static final long ONE_SECOND_MILLIS = 1000L;
    private static final long SECOND_BOUNDARY_MARGIN_MILLIS = 100L;
    private static final int REPEAT_CALLS = 5;
    private static final int PRESIGN_BENCH_CALLS = 20_000;
    private static final int PRESIGN_WARMUP_CALLS = 2_000;

    private S3StorageService storageService;

    @BeforeEach
    void setUp() {
        S3Properties s3Properties = new S3Properties(
            "test-bucket", "ap-northeast-2", "", "AKIAFAKEFAKEFAKEFAKE", "fakesecretfakesecretfakesecretfakesecret",
            "d111111abcdef8.cloudfront.net", "");
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
            AwsBasicCredentials.create(s3Properties.accessKey(), s3Properties.secretKey()));
        S3Presigner presigner = S3Presigner.builder()
            .region(Region.AP_NORTHEAST_2).credentialsProvider(credentials).build();
        S3Client s3Client = S3Client.builder()
            .region(Region.AP_NORTHEAST_2).credentialsProvider(credentials).build();
        ImageProperties imageProperties = new ImageProperties(
            new ImageProperties.ThumbnailProperties(THUMBNAIL_SIZE, THUMBNAIL_SIZE), EXPIRY_MINUTES, new ImageProperties.ResizeProperties(4, 30, 0.5, 0.4));
        storageService = new S3StorageService(
            s3Client, presigner, s3Properties, (FileProperties) null, imageProperties,
            new PresignedUrlCache(inMemoryRedis(), imageProperties, new SimpleMeterRegistry()));
    }

    @SuppressWarnings("unchecked")
    private RedisTemplate<String, String> inMemoryRedis() {
        Map<String, String> store = new ConcurrentHashMap<>();
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(ops.get(anyString())).thenAnswer(invocation -> store.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> store.put(invocation.getArgument(0), invocation.getArgument(1)))
            .when(ops).set(anyString(), anyString(), any(Duration.class));
        RedisTemplate<String, String> template = mock(RedisTemplate.class);
        when(template.opsForValue()).thenReturn(ops);
        return template;
    }

    @Test
    @DisplayName("PUBLIC 키는 몇 번을 불러도 URL이 같다")
    void publicUrlIsStable() throws InterruptedException {
        Set<String> urls = new HashSet<>();
        for (int i = 0; i < REPEAT_CALLS; i++) {
            urls.add(storageService.getUrl(PUBLIC_KEY));
            Thread.sleep(ONE_SECOND_MILLIS);
        }
        System.out.println("[MEASURE] PUBLIC distinct urls in " + REPEAT_CALLS + " calls over ~"
            + REPEAT_CALLS + "s = " + urls.size());
        assertThat(urls).hasSize(1);
    }

    @Test
    @DisplayName("PRIVATE 키는 초가 바뀔 때마다 URL이 달라진다")
    void privateUrlChangesEverySecond() throws InterruptedException {
        Set<String> urls = new HashSet<>();
        String first = storageService.getUrl(PRIVATE_KEY);
        String sameSecond = storageService.getUrl(PRIVATE_KEY);
        for (int i = 0; i < REPEAT_CALLS; i++) {
            urls.add(storageService.getUrl(PRIVATE_KEY));
            Thread.sleep(ONE_SECOND_MILLIS);
        }
        System.out.println("[MEASURE] PRIVATE distinct urls in " + REPEAT_CALLS + " calls over ~"
            + REPEAT_CALLS + "s = " + urls.size());
        System.out.println("[MEASURE] PRIVATE sample url = " + first);
        System.out.println("[MEASURE] PRIVATE same-second calls equal = " + first.equals(sameSecond));
        assertThat(urls.size()).isGreaterThan(1);
    }

    @Test
    @DisplayName("사용자별 캐시를 쓰면 PRIVATE도 초가 바뀌어도 URL이 같다")
    void privateUrlIsStableWithUserCache() throws InterruptedException {
        Set<String> urls = new HashSet<>();
        for (int i = 0; i < REPEAT_CALLS; i++) {
            urls.add(storageService.getUrl(USER_ID, PRIVATE_KEY));
            Thread.sleep(ONE_SECOND_MILLIS);
        }
        System.out.println("[MEASURE] PRIVATE with cache distinct urls in " + REPEAT_CALLS + " calls = " + urls.size());
        assertThat(urls).hasSize(1);
    }

    @Test
    @DisplayName("다른 사용자는 같은 PRIVATE 이미지 경로여도 각자 캐시 항목을 쓴다")
    void privateUrlIsCachedPerUser() throws InterruptedException {
        String first = storageService.getUrl(USER_ID, PRIVATE_KEY);
        Thread.sleep(ONE_SECOND_MILLIS + SECOND_BOUNDARY_MARGIN_MILLIS);

        String otherUser = storageService.getUrl(OTHER_USER_ID, PRIVATE_KEY);
        String sameUserAgain = storageService.getUrl(USER_ID, PRIVATE_KEY);

        assertThat(sameUserAgain).isEqualTo(first);
        assertThat(otherUser).isNotEqualTo(first);
    }

    @Test
    @DisplayName("PUBLIC 키와 사용자 없는 호출은 캐시를 거치지 않는다")
    void publicAndAnonymousSkipCache() throws InterruptedException {
        assertThat(storageService.getUrl(USER_ID, PUBLIC_KEY)).isEqualTo(storageService.getUrl(PUBLIC_KEY));

        String first = storageService.getUrl(null, PRIVATE_KEY);
        Thread.sleep(ONE_SECOND_MILLIS + 100);
        assertThat(storageService.getUrl(null, PRIVATE_KEY)).isNotEqualTo(first);
    }

    @Test
    @DisplayName("Presigned URL 생성 1건당 CPU 비용을 잰다")
    void presignCost() {
        for (int i = 0; i < PRESIGN_WARMUP_CALLS; i++) {
            storageService.getUrl(PRIVATE_KEY);
        }
        long start = System.nanoTime();
        for (int i = 0; i < PRESIGN_BENCH_CALLS; i++) {
            storageService.getUrl(PRIVATE_KEY);
        }
        double perCallMicros = (System.nanoTime() - start) / 1000.0 / PRESIGN_BENCH_CALLS;
        long startPublic = System.nanoTime();
        for (int i = 0; i < PRESIGN_BENCH_CALLS; i++) {
            storageService.getUrl(PUBLIC_KEY);
        }
        double publicMicros = (System.nanoTime() - startPublic) / 1000.0 / PRESIGN_BENCH_CALLS;
        System.out.printf("[MEASURE] presign per call = %.1f us, public per call = %.2f us%n", perCallMicros, publicMicros);
        assertThat(perCallMicros).isPositive();
    }
}
