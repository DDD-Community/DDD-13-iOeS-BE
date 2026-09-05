package com.ioes.photo.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.ioes.photo.domain.notification.dto.SpotOpenReviewHistoryCheckResponse;
import com.ioes.photo.domain.notification.dto.SpotOpenReviewHistoryListResponse;
import com.ioes.photo.domain.notification.entity.SpotOpenReviewHistory;
import com.ioes.photo.domain.notification.repository.SpotOpenReviewHistoryRepository;
import com.ioes.photo.domain.spot.dto.SpotReviewRequest;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.enums.RejectionReason;
import com.ioes.photo.domain.spot.enums.ReviewDecision;
import com.ioes.photo.domain.spot.enums.SpotOpenRequestStatus;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spot.service.SpotReviewService;
import com.ioes.photo.global.storage.S3StorageService;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * 스팟 검수완료 알림 히스토리 통합 테스트 — 실제 JPA(H2/PostgreSQL 모드)로 이벤트 기반 적재를 검증한다.
 *
 * @author 황제연
 */
@SpringBootTest
@DisplayName("스팟 검수완료 알림 히스토리 통합 테스트")
class SpotOpenReviewHistoryIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () ->
            "c2VjcmV0LWtleS1mb3ItdGVzdGluZy1wdXJwb3Nlcy1vbmx5LW11c3QtYmUtYXQtbGVhc3QtNjQtYnl0ZXMtbG9uZw==");
    }

    @MockitoBean RedisConnectionFactory redisConnectionFactory;
    @MockitoBean ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;
    @MockitoBean S3StorageService storageService;

    @Autowired SpotReviewService spotReviewService;
    @Autowired SpotRepository spotRepository;
    @Autowired SpotOpenReviewHistoryRepository spotOpenReviewHistoryRepository;
    @Autowired SpotOpenReviewHistoryService spotOpenReviewHistoryService;
    @Autowired JdbcTemplate jdbcTemplate;

    private static final Long USER_ID = 1L;
    private static final Long REVIEWER_ID = 900L;
    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration AWAIT_POLL_INTERVAL = Duration.ofMillis(100);

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM spot_open_review_history");
        jdbcTemplate.execute("DELETE FROM spot_open_requests");
        jdbcTemplate.execute("DELETE FROM spot_reviews");
        jdbcTemplate.execute("DELETE FROM spots");
    }

    @Test
    @DisplayName("승인 검수가 커밋되면 승인 히스토리가 미확인 상태로 적재된다")
    void approveCreatesUncheckedApprovedHistory() {
        Long spotId = saveSpot(SpotStatus.PENDING, USER_ID).getId();

        spotReviewService.review(spotId, approve(), REVIEWER_ID);

        // 히스토리 적재는 @Async 리스너에서 처리되므로, 별도 스레드의 커밋을 기다렸다가 검증한다.
        await().atMost(AWAIT_TIMEOUT).pollInterval(AWAIT_POLL_INTERVAL).untilAsserted(() -> {
            List<SpotOpenReviewHistory> histories = spotOpenReviewHistoryRepository.findAll();
            assertThat(histories).singleElement().satisfies(history -> {
                assertThat(history.getSpotId()).isEqualTo(spotId);
                assertThat(history.getUserId()).isEqualTo(USER_ID);
                assertThat(history.getSpotStatus()).isEqualTo(SpotOpenRequestStatus.APPROVED);
                assertThat(history.getRejectReason()).isNull();
                assertThat(history.getCheckYn().getCode()).isEqualTo("N");
            });
        });
    }

    @Test
    @DisplayName("반려 검수가 커밋되면 반려 사유·상세와 함께 히스토리가 적재된다")
    void rejectCreatesUncheckedRejectedHistory() {
        Long spotId = saveSpot(SpotStatus.PENDING, USER_ID).getId();

        spotReviewService.review(spotId,
            new SpotReviewRequest(ReviewDecision.REJECTED, RejectionReason.ETC, "상세 사유"), REVIEWER_ID);

        await().atMost(AWAIT_TIMEOUT).pollInterval(AWAIT_POLL_INTERVAL).untilAsserted(() -> {
            List<SpotOpenReviewHistory> histories = spotOpenReviewHistoryRepository.findAll();
            assertThat(histories).singleElement().satisfies(history -> {
                assertThat(history.getSpotStatus()).isEqualTo(SpotOpenRequestStatus.REJECTED);
                assertThat(history.getRejectReason()).isEqualTo(RejectionReason.ETC);
                assertThat(history.getRejectDetail()).isEqualTo("상세 사유");
            });
        });
    }

    @Test
    @DisplayName("소유자가 없는 큐레이션 스팟은 검수완료되어도 히스토리가 남지 않는다")
    void curatedSpotWithoutOwnerSkipsHistory() {
        Long spotId = saveSpot(SpotStatus.PENDING, null).getId();

        spotReviewService.review(spotId, approve(), REVIEWER_ID);

        assertThat(spotOpenReviewHistoryRepository.findAll()).isEmpty();
    }

    @Test
    @Transactional
    @DisplayName("검수 트랜잭션이 커밋되지 않으면(AFTER_COMMIT 이전) 히스토리도 적재되지 않는다")
    void historyIsNotCreatedBeforeCommit() {
        Long spotId = saveSpot(SpotStatus.PENDING, USER_ID).getId();

        spotReviewService.review(spotId, approve(), REVIEWER_ID);

        assertThat(spotRepository.findById(spotId)).get()
            .extracting(Spot::getStatus).isEqualTo(SpotStatus.PUBLISHED);
        assertThat(spotOpenReviewHistoryRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("확인 처리한 히스토리는 미확인 목록 조회에서 제외된다")
    void markCheckedExcludesFromUncheckedList() {
        Long spotId = saveSpot(SpotStatus.PENDING, USER_ID).getId();
        spotReviewService.review(spotId, approve(), REVIEWER_ID);

        Long historyId = await().atMost(AWAIT_TIMEOUT).pollInterval(AWAIT_POLL_INTERVAL)
            .until(
                () -> spotOpenReviewHistoryRepository.findAll().stream().findFirst().map(SpotOpenReviewHistory::getId),
                Optional::isPresent)
            .orElseThrow();

        SpotOpenReviewHistoryCheckResponse checkResponse =
            spotOpenReviewHistoryService.markChecked(USER_ID, historyId);
        assertThat(checkResponse.checkYn()).isEqualTo("Y");

        SpotOpenReviewHistoryListResponse listResponse = spotOpenReviewHistoryService.findUnchecked(USER_ID);
        assertThat(listResponse.approved()).isEmpty();
        assertThat(listResponse.rejected()).isEmpty();
    }

    @Test
    @DisplayName("승인/반려 히스토리가 섞여 있으면 findUnchecked가 각각의 목록으로 분리해 반환한다")
    void findUncheckedSplitsApprovedAndRejected() {
        Long approvedSpotId = saveSpot(SpotStatus.PENDING, USER_ID).getId();
        spotReviewService.review(approvedSpotId, approve(), REVIEWER_ID);

        Long rejectedSpotId = saveSpot(SpotStatus.PENDING, USER_ID).getId();
        spotReviewService.review(rejectedSpotId,
            new SpotReviewRequest(ReviewDecision.REJECTED, RejectionReason.LOW_QUALITY, null), REVIEWER_ID);

        await().atMost(AWAIT_TIMEOUT).pollInterval(AWAIT_POLL_INTERVAL)
            .until(() -> spotOpenReviewHistoryRepository.findAll().size() == 2);

        SpotOpenReviewHistoryListResponse response = spotOpenReviewHistoryService.findUnchecked(USER_ID);

        assertThat(response.approved()).extracting(item -> item.spotId()).containsExactly(approvedSpotId);
        assertThat(response.rejected()).extracting(item -> item.spotId()).containsExactly(rejectedSpotId);
        assertThat(response.rejected().get(0).rejectReasonLabel()).isEqualTo("사진 상태 불량");
    }

    private static SpotReviewRequest approve() {
        return new SpotReviewRequest(ReviewDecision.APPROVED, null, null);
    }

    private Spot saveSpot(SpotStatus status, Long userId) {
        return spotRepository.save(Spot.builder()
            .name("검수완료 히스토리 통합테스트 스팟")
            .theme(SpotTheme.NIGHT_VIEW)
            .latitude(37.5326)
            .longitude(126.9905)
            .status(status)
            .userId(userId)
            .build());
    }
}
