package com.ioes.photo.domain.myspot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ioes.photo.domain.spot.dto.SpotReviewRequest;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotOpenRequest;
import com.ioes.photo.domain.spot.enums.RejectionReason;
import com.ioes.photo.domain.spot.enums.ReviewDecision;
import com.ioes.photo.domain.spot.enums.SpotOpenRequestStatus;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.error.SpotErrorCode;
import com.ioes.photo.domain.spot.repository.SpotOpenRequestRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spot.service.SpotReviewService;
import com.ioes.photo.global.error.exception.BusinessException;
import com.ioes.photo.global.storage.S3StorageService;
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

/**
 * 스팟 공개 라이프사이클 통합 테스트 — 실제 JPA + H2로 검증.
 *
 * 등록 → 오픈 신청 → 철회 → 재신청 → 승인 → 비공개 전환 전 구간에서
 * 스팟 상태와 오픈 신청 이력이 함께 맞물려 움직이는지 확인한다.
 *
 * @author 황제연
 */
@SpringBootTest
@DisplayName("스팟 공개 라이프사이클 통합 테스트")
class MySpotPublicationFlowIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () ->
            "c2VjcmV0LWtleS1mb3ItdGVzdGluZy1wdXJwb3Nlcy1vbmx5LW11c3QtYmUtYXQtbGVhc3QtNjQtYnl0ZXMtbG9uZw==");
    }

    @MockitoBean RedisConnectionFactory redisConnectionFactory;
    @MockitoBean ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;
    @MockitoBean S3StorageService storageService;

    @Autowired MySpotService mySpotService;
    @Autowired SpotReviewService spotReviewService;
    @Autowired SpotRepository spotRepository;
    @Autowired SpotOpenRequestRepository spotOpenRequestRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private static final Long USER_ID = 1L;
    private static final Long REVIEWER_ID = 900L;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM spot_open_requests");
        jdbcTemplate.execute("DELETE FROM spot_reviews");
        jdbcTemplate.execute("DELETE FROM spot_alarm_subscriptions");
        jdbcTemplate.execute("DELETE FROM spots");
    }

    @Test
    @DisplayName("신청 → 철회 → 재신청 → 승인 → 비공개 전환 전 구간에서 상태와 이력이 함께 움직인다")
    void fullPublicationLifecycle() {
        Spot spot = saveSpot(SpotStatus.DRAFT);
        Long spotId = spot.getId();

        mySpotService.requestOpen(USER_ID, spotId);
        assertThat(reload(spotId).getStatus()).isEqualTo(SpotStatus.PENDING);
        assertThat(inFlightRequest(spotId)).isPresent();

        mySpotService.cancelPublication(USER_ID, spotId);
        assertThat(reload(spotId).getStatus()).isEqualTo(SpotStatus.DRAFT);
        assertThat(reload(spotId).getAppliedAt()).isNull();
        assertThat(inFlightRequest(spotId)).isEmpty();
        assertThat(historyOf(spotId)).singleElement()
            .extracting(SpotOpenRequest::getStatus)
            .isEqualTo(SpotOpenRequestStatus.CANCELED);

        mySpotService.requestOpen(USER_ID, spotId);
        assertThat(reload(spotId).getStatus()).isEqualTo(SpotStatus.PENDING);
        assertThat(historyOf(spotId)).hasSize(2);

        spotReviewService.review(spotId, approve(), REVIEWER_ID);
        assertThat(reload(spotId).getStatus()).isEqualTo(SpotStatus.PUBLISHED);
        assertThat(historyOf(spotId)).first()
            .satisfies(request -> {
                assertThat(request.getStatus()).isEqualTo(SpotOpenRequestStatus.APPROVED);
                assertThat(request.getSpotReviewId()).isNotNull();
                assertThat(request.getResolvedAt()).isNotNull();
            });

        mySpotService.cancelPublication(USER_ID, spotId);
        assertThat(reload(spotId).getStatus()).isEqualTo(SpotStatus.DRAFT);
        assertThat(historyOf(spotId)).hasSize(2);
    }

    @Test
    @DisplayName("반려 후 재신청하면 재검토대기로 전이되고 이력이 한 건 더 쌓인다")
    void reRequestAfterRejection() {
        Long spotId = saveSpot(SpotStatus.DRAFT).getId();

        mySpotService.requestOpen(USER_ID, spotId);
        spotReviewService.review(spotId,
            new SpotReviewRequest(ReviewDecision.REJECTED, RejectionReason.LOW_QUALITY, null), REVIEWER_ID);

        assertThat(reload(spotId).getStatus()).isEqualTo(SpotStatus.REJECTED);
        assertThat(historyOf(spotId)).singleElement()
            .extracting(SpotOpenRequest::getStatus)
            .isEqualTo(SpotOpenRequestStatus.REJECTED);

        mySpotService.requestOpen(USER_ID, spotId);

        assertThat(reload(spotId).getStatus()).isEqualTo(SpotStatus.RE_REVIEW_PENDING);
        assertThat(historyOf(spotId)).hasSize(2);
        assertThat(inFlightRequest(spotId)).isPresent();
    }

    @Test
    @DisplayName("검수가 먼저 확정되면 뒤늦은 철회는 이미 처리된 신청으로 안내한다")
    void cancelAfterReviewIsRejected() {
        Long spotId = saveSpot(SpotStatus.DRAFT).getId();
        mySpotService.requestOpen(USER_ID, spotId);
        spotReviewService.review(spotId,
            new SpotReviewRequest(ReviewDecision.REJECTED, RejectionReason.DUPLICATE, null), REVIEWER_ID);

        assertThatThrownBy(() -> mySpotService.cancelPublication(USER_ID, spotId))
            .isInstanceOf(BusinessException.class)
            .hasMessage("이미 처리된 신청이에요.")
            .extracting("errorCode")
            .isEqualTo(SpotErrorCode.SPOT_ALREADY_REVIEWED);
    }

    @Test
    @DisplayName("철회로 DRAFT가 된 스팟은 운영자가 더 이상 검수할 수 없다")
    void reviewAfterCancelIsRejected() {
        Long spotId = saveSpot(SpotStatus.DRAFT).getId();
        mySpotService.requestOpen(USER_ID, spotId);
        mySpotService.cancelPublication(USER_ID, spotId);

        assertThatThrownBy(() -> spotReviewService.review(spotId, approve(), REVIEWER_ID))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(SpotErrorCode.SPOT_ALREADY_REVIEWED);
    }

    @Test
    @DisplayName("비공개로 전환해도 이미 쌓인 좋아요/북마크 수는 그대로 남는다")
    void keepsReactionsOnUnpublish() {
        Spot spot = saveSpot(SpotStatus.PUBLISHED);
        Long spotId = spot.getId();
        jdbcTemplate.update("UPDATE spots SET like_count = 1, bookmark_count = 1 WHERE id = ?", spotId);

        mySpotService.cancelPublication(USER_ID, spotId);

        Spot unpublished = reload(spotId);
        assertThat(unpublished.getStatus()).isEqualTo(SpotStatus.DRAFT);
        assertThat(unpublished.getLikeCount()).isEqualTo(1L);
        assertThat(unpublished.getBookmarkCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("삭제한 스팟은 일반 조회에서 빠지고 검수 대상도 되지 않는다")
    void deletedSpotDisappearsFromQueries() {
        Long spotId = saveSpot(SpotStatus.PUBLISHED).getId();

        mySpotService.deleteMySpot(USER_ID, spotId);

        assertThat(spotRepository.findById(spotId)).isEmpty();
        assertThat(spotRepository.findByIdIncludingDeleted(spotId)).isPresent();
        assertThatThrownBy(() -> spotReviewService.review(spotId, approve(), REVIEWER_ID))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(SpotErrorCode.SPOT_NOT_FOUND);
    }

    @Test
    @DisplayName("검수 중인 스팟은 삭제할 수 없다")
    void cannotDeleteSpotUnderReview() {
        Long spotId = saveSpot(SpotStatus.DRAFT).getId();
        mySpotService.requestOpen(USER_ID, spotId);

        assertThatThrownBy(() -> mySpotService.deleteMySpot(USER_ID, spotId))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(SpotErrorCode.SPOT_NOT_DELETABLE);

        assertThat(spotRepository.findById(spotId)).isPresent();
    }

    private static SpotReviewRequest approve() {
        return new SpotReviewRequest(ReviewDecision.APPROVED, null, null);
    }

    private Spot saveSpot(SpotStatus status) {
        return spotRepository.save(Spot.builder()
            .name("통합테스트 스팟")
            .theme(SpotTheme.NIGHT_VIEW)
            .latitude(37.5326)
            .longitude(126.9905)
            .status(status)
            .userId(USER_ID)
            .build());
    }

    private Spot reload(Long spotId) {
        return spotRepository.findById(spotId).orElseThrow();
    }

    private List<SpotOpenRequest> historyOf(Long spotId) {
        return spotOpenRequestRepository.findBySpotIdOrderByRequestedAtDesc(spotId);
    }

    private Optional<SpotOpenRequest> inFlightRequest(Long spotId) {
        return spotOpenRequestRepository.findFirstBySpotIdAndStatusOrderByRequestedAtDesc(
            spotId, SpotOpenRequestStatus.REQUESTED);
    }
}
