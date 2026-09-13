package com.ioes.photo.domain.spotlike.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spotlike.dto.SpotLikeResponse;
import com.ioes.photo.domain.spotlike.error.SpotLikeErrorCode;
import com.ioes.photo.domain.spotlike.repository.SpotLikeRepository;
import com.ioes.photo.global.error.exception.BusinessException;
import com.ioes.photo.global.storage.S3StorageService;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * {@link SpotLikeService} 통합 테스트 — 실제 JPA + H2로 좋아요 카운터 정합성 검증.
 *
 * @author 황제연
 */
@SpringBootTest
@DisplayName("SpotLikeService 통합 테스트")
class SpotLikeServiceIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () ->
            "c2VjcmV0LWtleS1mb3ItdGVzdGluZy1wdXJwb3Nlcy1vbmx5LW11c3QtYmUtYXQtbGVhc3QtNjQtYnl0ZXMtbG9uZw==");
    }

    @MockitoBean RedisConnectionFactory redisConnectionFactory;
    @MockitoBean ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;
    @MockitoBean S3StorageService storageService;

    @Autowired SpotLikeService spotLikeService;
    @Autowired SpotLikeRepository spotLikeRepository;
    @Autowired SpotRepository spotRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private static final Long USER_ID = 1L;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM spot_likes");
        jdbcTemplate.execute("DELETE FROM spots");
    }

    @Test
    @DisplayName("좋아요하면 spot_likes에 레코드가 남고 like_count가 1 증가한다")
    void addLikeSavesRecordAndIncrementsCount() {
        Spot spot = saveSpot(SpotStatus.PUBLISHED, USER_ID + 1);

        SpotLikeResponse response = spotLikeService.addLike(USER_ID, spot.getId());

        assertThat(response.likeCount()).isEqualTo(1L);
        assertThat(response.isLiked()).isTrue();
        assertThat(spotLikeRepository.findByUserIdAndSpotId(USER_ID, spot.getId())).isPresent();
        assertThat(reload(spot.getId()).getLikeCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("좋아요를 취소하면 논리삭제되고 like_count가 1 감소한다")
    void removeLikeSoftDeletesAndDecrementsCount() {
        Spot spot = saveSpot(SpotStatus.PUBLISHED, USER_ID + 1);
        spotLikeService.addLike(USER_ID, spot.getId());

        SpotLikeResponse response = spotLikeService.removeLike(USER_ID, spot.getId());

        assertThat(response.likeCount()).isZero();
        assertThat(response.isLiked()).isFalse();
        assertThat(spotLikeRepository.findByUserIdAndSpotId(USER_ID, spot.getId())).isEmpty();
        assertThat(spotLikeRepository.findByUserIdAndSpotIdIncludingDeleted(USER_ID, spot.getId())).isPresent();
    }

    @Test
    @DisplayName("취소했다가 다시 좋아요하면 기존 레코드를 되살려 중복 행이 생기지 않는다")
    void reLikeRestoresExistingRow() {
        Spot spot = saveSpot(SpotStatus.PUBLISHED, USER_ID + 1);
        spotLikeService.addLike(USER_ID, spot.getId());
        spotLikeService.removeLike(USER_ID, spot.getId());

        SpotLikeResponse response = spotLikeService.addLike(USER_ID, spot.getId());

        assertThat(response.likeCount()).isEqualTo(1L);
        Long rowCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM spot_likes WHERE user_id = ? AND spot_id = ?",
            Long.class, USER_ID, spot.getId());
        assertThat(rowCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("이미 좋아요한 스팟에 다시 요청하면 409로 막고 카운터를 올리지 않는다")
    void duplicateLikeIsRejected() {
        Spot spot = saveSpot(SpotStatus.PUBLISHED, USER_ID + 1);
        spotLikeService.addLike(USER_ID, spot.getId());

        assertThatThrownBy(() -> spotLikeService.addLike(USER_ID, spot.getId()))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(SpotLikeErrorCode.ALREADY_LIKED);

        assertThat(reload(spot.getId()).getLikeCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("승인되지 않은 유저 스팟에는 좋아요할 수 없다")
    void cannotLikeUnpublishedUserSpot() {
        Spot spot = saveSpot(SpotStatus.DRAFT, USER_ID + 1);

        assertThatThrownBy(() -> spotLikeService.addLike(USER_ID, spot.getId()))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(SpotLikeErrorCode.SPOT_NOT_LIKEABLE);
    }

    @Test
    @DisplayName("관리자 큐레이션 스팟은 상태와 무관하게 좋아요할 수 있다")
    void canLikeCuratedSpotRegardlessOfStatus() {
        Spot spot = saveSpot(SpotStatus.DRAFT, null);

        SpotLikeResponse response = spotLikeService.addLike(USER_ID, spot.getId());

        assertThat(response.likeCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("여러 사용자가 동시에 좋아요해도 최종 like_count가 요청 수와 일치한다")
    void concurrentLikesKeepCounterConsistent() throws InterruptedException {
        Spot spot = saveSpot(SpotStatus.PUBLISHED, 999L);
        int threadCount = 8;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger succeeded = new AtomicInteger();

        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            for (long userId = 1; userId <= threadCount; userId++) {
                long likerId = userId;
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        spotLikeService.addLike(likerId, spot.getId());
                        succeeded.incrementAndGet();
                    } catch (Exception ignored) {
                        // 경합으로 실패한 요청은 최종 카운터 비교에서 제외한다
                    } finally {
                        done.countDown();
                    }
                });
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(reload(spot.getId()).getLikeCount()).isEqualTo(succeeded.get());
    }

    @Test
    @DisplayName("좋아요한 스팟 ID 목록을 한 번에 조회한다")
    void findsLikedSpotIdsInBatch() {
        Spot liked = saveSpot(SpotStatus.PUBLISHED, 999L);
        Spot notLiked = saveSpot(SpotStatus.PUBLISHED, 999L);
        spotLikeService.addLike(USER_ID, liked.getId());

        var likedIds = spotLikeRepository.findLikedSpotIds(
            USER_ID, List.of(liked.getId(), notLiked.getId()));

        assertThat(likedIds).containsExactly(liked.getId());
    }

    private Spot saveSpot(SpotStatus status, Long ownerId) {
        return spotRepository.save(Spot.builder()
            .name("통합테스트 스팟")
            .theme(SpotTheme.SUNLIGHT)
            .latitude(37.5326)
            .longitude(126.9905)
            .status(status)
            .userId(ownerId)
            .build());
    }

    private Spot reload(Long spotId) {
        return spotRepository.findById(spotId).orElseThrow();
    }
}
