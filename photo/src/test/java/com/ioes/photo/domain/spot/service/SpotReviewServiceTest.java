package com.ioes.photo.domain.spot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ioes.photo.domain.spot.dto.SpotReviewRequest;
import com.ioes.photo.domain.spot.dto.SpotReviewResultResponse;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotOpenRequest;
import com.ioes.photo.domain.spot.entity.SpotReview;
import com.ioes.photo.domain.spot.enums.RejectionReason;
import com.ioes.photo.domain.spot.enums.ReviewDecision;
import com.ioes.photo.domain.spot.enums.SpotOpenRequestStatus;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.error.SpotErrorCode;
import com.ioes.photo.domain.spot.event.SpotOpenReviewCompletedEvent;
import com.ioes.photo.domain.spot.repository.SpotOpenRequestRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spot.repository.SpotReviewRepository;
import com.ioes.photo.global.error.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * {@link SpotReviewService} 단위 테스트.
 *
 * 이미지 공개 전환의 세부 동작은 {@link SpotImageAccessServiceTest} 가 담당하고,
 * 여기서는 위임 여부만 확인한다.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpotReviewService 단위 테스트")
class SpotReviewServiceTest {

    @Mock SpotRepository            spotRepository;
    @Mock SpotReviewRepository      spotReviewRepository;
    @Mock SpotOpenRequestRepository spotOpenRequestRepository;
    @Mock SpotImageAccessService    spotImageAccessService;
    @Mock ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks SpotReviewService spotReviewService;

    private static final Long SPOT_ID     = 7L;
    private static final Long USER_ID     = 1L;
    private static final Long REVIEWER_ID = 99L;

    @Nested
    @DisplayName("승인")
    class Approve {

        @Test
        @DisplayName("검수중 스팟을 승인하면 PUBLISHED 로 전이되고 승인 이력을 저장한다")
        void approvePendingSpot() {
            Spot spot = buildSpot(SpotStatus.PENDING);
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(spot));

            SpotReviewResultResponse response =
                spotReviewService.review(SPOT_ID, approveRequest(), REVIEWER_ID);

            assertThat(response.status()).isEqualTo(SpotStatus.PUBLISHED.name());
            assertThat(spot.getStatus()).isEqualTo(SpotStatus.PUBLISHED);
            assertThat(spot.getReviewerId()).isEqualTo(REVIEWER_ID);
            assertThat(spot.getReviewedAt()).isNotNull();
            assertThat(spot.isReleased()).isTrue();

            ArgumentCaptor<SpotReview> captor = ArgumentCaptor.forClass(SpotReview.class);
            then(spotReviewRepository).should().saveAndFlush(captor.capture());
            assertThat(captor.getValue().getDecision()).isEqualTo(ReviewDecision.APPROVED);
        }

        @Test
        @DisplayName("승인 시 이미지 공개 전환을 이미지 접근 서비스에 위임한다")
        void delegatesImagePublish() {
            Spot spot = buildSpot(SpotStatus.PENDING);
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(spot));

            spotReviewService.review(SPOT_ID, approveRequest(), REVIEWER_ID);

            then(spotImageAccessService).should().publish(SPOT_ID);
        }

        @Test
        @DisplayName("승인 시 검수완료 이벤트를 APPROVED 상태로, 반려 사유 없이 발행한다")
        void publishesApprovedEvent() {
            Spot spot = buildSpot(SpotStatus.PENDING);
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(spot));

            spotReviewService.review(SPOT_ID, approveRequest(), REVIEWER_ID);

            ArgumentCaptor<SpotOpenReviewCompletedEvent> captor =
                ArgumentCaptor.forClass(SpotOpenReviewCompletedEvent.class);
            then(applicationEventPublisher).should().publishEvent(captor.capture());
            SpotOpenReviewCompletedEvent event = captor.getValue();
            assertThat(event.spotId()).isEqualTo(SPOT_ID);
            assertThat(event.userId()).isEqualTo(USER_ID);
            assertThat(event.status()).isEqualTo(SpotOpenRequestStatus.APPROVED);
            assertThat(event.rejectReason()).isNull();
            assertThat(event.rejectDetail()).isNull();
        }

        @Test
        @DisplayName("진행 중이던 오픈 신청 이력을 승인으로 마감하고 검수 건과 연결한다")
        void resolvesOpenRequestAsApproved() {
            Spot spot = buildSpot(SpotStatus.PENDING);
            SpotOpenRequest openRequest = buildOpenRequest();
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(spot));
            given(spotOpenRequestRepository.findFirstBySpotIdAndStatusOrderByRequestedAtDesc(
                SPOT_ID, SpotOpenRequestStatus.REQUESTED)).willReturn(Optional.of(openRequest));
            givenReviewIdAssignedOnSave(11L);

            spotReviewService.review(SPOT_ID, approveRequest(), REVIEWER_ID);

            assertThat(openRequest.getStatus()).isEqualTo(SpotOpenRequestStatus.APPROVED);
            assertThat(openRequest.getSpotReviewId()).isEqualTo(11L);
            assertThat(openRequest.getResolvedAt()).isEqualTo(spot.getReviewedAt());
            assertThat(openRequest.isInFlight()).isFalse();
        }
    }

    @Nested
    @DisplayName("반려")
    class Reject {

        @Test
        @DisplayName("사유와 함께 반려하면 REJECTED 로 전이되고 반려 이력을 저장한다")
        void rejectWithReason() {
            Spot spot = buildSpot(SpotStatus.PENDING);
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(spot));

            SpotReviewRequest request =
                new SpotReviewRequest(ReviewDecision.REJECTED, RejectionReason.LOW_QUALITY, null);
            spotReviewService.review(SPOT_ID, request, REVIEWER_ID);

            assertThat(spot.getStatus()).isEqualTo(SpotStatus.REJECTED);

            ArgumentCaptor<SpotReview> captor = ArgumentCaptor.forClass(SpotReview.class);
            then(spotReviewRepository).should().saveAndFlush(captor.capture());
            assertThat(captor.getValue().getReason()).isEqualTo(RejectionReason.LOW_QUALITY);
            then(spotImageAccessService).should(never()).publish(anyLong());
        }

        @Test
        @DisplayName("반려 시 검수완료 이벤트를 REJECTED 상태로, 반려 사유와 함께 발행한다")
        void publishesRejectedEvent() {
            Spot spot = buildSpot(SpotStatus.PENDING);
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(spot));

            spotReviewService.review(SPOT_ID,
                new SpotReviewRequest(ReviewDecision.REJECTED, RejectionReason.LOW_QUALITY, "상세"), REVIEWER_ID);

            ArgumentCaptor<SpotOpenReviewCompletedEvent> captor =
                ArgumentCaptor.forClass(SpotOpenReviewCompletedEvent.class);
            then(applicationEventPublisher).should().publishEvent(captor.capture());
            SpotOpenReviewCompletedEvent event = captor.getValue();
            assertThat(event.status()).isEqualTo(SpotOpenRequestStatus.REJECTED);
            assertThat(event.rejectReason()).isEqualTo(RejectionReason.LOW_QUALITY);
            assertThat(event.rejectDetail()).isEqualTo("상세");
        }

        @Test
        @DisplayName("진행 중이던 오픈 신청 이력을 반려로 마감한다")
        void resolvesOpenRequestAsRejected() {
            Spot spot = buildSpot(SpotStatus.RE_REVIEW_PENDING);
            SpotOpenRequest openRequest = buildOpenRequest();
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(spot));
            given(spotOpenRequestRepository.findFirstBySpotIdAndStatusOrderByRequestedAtDesc(
                SPOT_ID, SpotOpenRequestStatus.REQUESTED)).willReturn(Optional.of(openRequest));

            spotReviewService.review(SPOT_ID,
                new SpotReviewRequest(ReviewDecision.REJECTED, RejectionReason.DUPLICATE, null), REVIEWER_ID);

            assertThat(openRequest.getStatus()).isEqualTo(SpotOpenRequestStatus.REJECTED);
        }

        @Test
        @DisplayName("반려 사유가 없으면 SPOT_REJECTION_REASON_REQUIRED 예외를 던진다")
        void throwsWhenReasonMissing() {
            Spot spot = buildSpot(SpotStatus.PENDING);
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(spot));

            SpotReviewRequest request = new SpotReviewRequest(ReviewDecision.REJECTED, null, null);

            assertThatThrownBy(() -> spotReviewService.review(SPOT_ID, request, REVIEWER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(SpotErrorCode.SPOT_REJECTION_REASON_REQUIRED));
        }

        @Test
        @DisplayName("기타(ETC) 사유인데 상세 설명이 없으면 SPOT_REJECTION_DETAIL_REQUIRED 예외를 던진다")
        void throwsWhenEtcDetailMissing() {
            Spot spot = buildSpot(SpotStatus.PENDING);
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(spot));

            SpotReviewRequest request =
                new SpotReviewRequest(ReviewDecision.REJECTED, RejectionReason.ETC, "  ");

            assertThatThrownBy(() -> spotReviewService.review(SPOT_ID, request, REVIEWER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(SpotErrorCode.SPOT_REJECTION_DETAIL_REQUIRED));
        }
    }

    @Nested
    @DisplayName("검수 가드")
    class Guard {

        @Test
        @DisplayName("행 잠금으로 조회해 사용자의 철회와 동시에 처리되지 않도록 한다")
        void readsWithPessimisticLock() {
            given(spotRepository.findWithLockById(SPOT_ID))
                .willReturn(Optional.of(buildSpot(SpotStatus.PENDING)));

            spotReviewService.review(SPOT_ID, approveRequest(), REVIEWER_ID);

            then(spotRepository).should().findWithLockById(SPOT_ID);
            then(spotRepository).should(never()).findById(anyLong());
        }

        @Test
        @DisplayName("이미 처리(PUBLISHED)된 스팟을 다시 처리하면 SPOT_ALREADY_REVIEWED 예외를 던진다")
        void throwsWhenAlreadyReviewed() {
            given(spotRepository.findWithLockById(SPOT_ID))
                .willReturn(Optional.of(buildSpot(SpotStatus.PUBLISHED)));

            assertThatThrownBy(() -> spotReviewService.review(SPOT_ID, approveRequest(), REVIEWER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(SpotErrorCode.SPOT_ALREADY_REVIEWED));

            then(spotReviewRepository).should(never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("사용자가 먼저 철회해 DRAFT 가 된 스팟은 SPOT_ALREADY_REVIEWED 예외를 던진다")
        void throwsWhenAlreadyCanceledByUser() {
            given(spotRepository.findWithLockById(SPOT_ID))
                .willReturn(Optional.of(buildSpot(SpotStatus.DRAFT)));

            assertThatThrownBy(() -> spotReviewService.review(SPOT_ID, approveRequest(), REVIEWER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(SpotErrorCode.SPOT_ALREADY_REVIEWED));
        }

        @Test
        @DisplayName("존재하지 않는 스팟이면 SPOT_NOT_FOUND 예외를 던진다")
        void throwsWhenSpotNotFound() {
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> spotReviewService.review(SPOT_ID, approveRequest(), REVIEWER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(SpotErrorCode.SPOT_NOT_FOUND));
        }

        @Test
        @DisplayName("검수 도입 이전 스팟처럼 진행 중 신청 이력이 없어도 검수는 정상 처리된다")
        void succeedsWithoutOpenRequestHistory() {
            Spot spot = buildSpot(SpotStatus.PENDING);
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(spot));
            given(spotOpenRequestRepository.findFirstBySpotIdAndStatusOrderByRequestedAtDesc(
                SPOT_ID, SpotOpenRequestStatus.REQUESTED)).willReturn(Optional.empty());

            spotReviewService.review(SPOT_ID, approveRequest(), REVIEWER_ID);

            assertThat(spot.getStatus()).isEqualTo(SpotStatus.PUBLISHED);
        }
    }

    private void givenReviewIdAssignedOnSave(Long reviewId) {
        given(spotReviewRepository.saveAndFlush(any(SpotReview.class))).willAnswer(invocation -> {
            SpotReview review = invocation.getArgument(0);
            ReflectionTestUtils.setField(review, "id", reviewId);
            return review;
        });
    }

    private static SpotReviewRequest approveRequest() {
        return new SpotReviewRequest(ReviewDecision.APPROVED, null, null);
    }

    private static SpotOpenRequest buildOpenRequest() {
        return SpotOpenRequest.request(SPOT_ID, USER_ID, LocalDateTime.now().minusHours(1));
    }

    private static Spot buildSpot(SpotStatus status) {
        Spot spot = Spot.builder()
            .name("테스트스팟")
            .theme(SpotTheme.SUNSET)
            .latitude(37.5)
            .longitude(127.0)
            .status(status)
            .userId(USER_ID)
            .build();
        ReflectionTestUtils.setField(spot, "id", SPOT_ID);
        return spot;
    }
}
