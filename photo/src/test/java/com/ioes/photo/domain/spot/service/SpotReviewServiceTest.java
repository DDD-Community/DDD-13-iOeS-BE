package com.ioes.photo.domain.spot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ioes.photo.domain.spot.dto.SpotReviewRequest;
import com.ioes.photo.domain.spot.dto.SpotReviewResultResponse;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.entity.SpotReview;
import com.ioes.photo.domain.spot.enums.RejectionReason;
import com.ioes.photo.domain.spot.enums.ReviewDecision;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.error.SpotErrorCode;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spot.repository.SpotReviewRepository;
import com.ioes.photo.global.error.exception.BusinessException;
import com.ioes.photo.global.storage.StorageService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * {@link SpotReviewService} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpotReviewService 단위 테스트")
class SpotReviewServiceTest {

    @Mock SpotRepository       spotRepository;
    @Mock SpotReviewRepository spotReviewRepository;
    @Mock SpotImageRepository  spotImageRepository;
    @Mock StorageService       storageService;

    @InjectMocks SpotReviewService spotReviewService;

    private static final Long SPOT_ID     = 7L;
    private static final Long REVIEWER_ID  = 99L;
    private static final String PRIVATE_KEY   = "prod/private/spots/7/original/202607/abc.jpg";
    private static final String PRIVATE_THUMB = "prod/private/spots/7/thumbnail/202607/abc.jpg";

    @Nested
    @DisplayName("승인")
    class Approve {

        @Test
        @DisplayName("검수중 스팟을 승인하면 PUBLISHED 로 전이되고 승인 이력을 저장한다")
        void approvePendingSpot() {
            Spot spot = buildSpot(SpotStatus.PENDING);
            given(spotRepository.findById(SPOT_ID)).willReturn(Optional.of(spot));
            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.empty());

            SpotReviewResultResponse response =
                spotReviewService.review(SPOT_ID, approveRequest(), REVIEWER_ID);

            assertThat(response.status()).isEqualTo(SpotStatus.PUBLISHED.name());
            assertThat(spot.getStatus()).isEqualTo(SpotStatus.PUBLISHED);
            assertThat(spot.getReviewerId()).isEqualTo(REVIEWER_ID);

            ArgumentCaptor<SpotReview> captor = ArgumentCaptor.forClass(SpotReview.class);
            then(spotReviewRepository).should().save(captor.capture());
            assertThat(captor.getValue().getDecision()).isEqualTo(ReviewDecision.APPROVED);
        }

        @Test
        @DisplayName("승인 시 미승인(private) 이미지를 public 경로로 이동한다")
        void movesImagesToPublicOnApprove() {
            Spot spot = buildSpot(SpotStatus.PENDING);
            SpotImage image = SpotImage.create(SPOT_ID, PRIVATE_KEY);
            image.updateThumbnailKey(PRIVATE_THUMB);

            given(spotRepository.findById(SPOT_ID)).willReturn(Optional.of(spot));
            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.of(image));

            spotReviewService.review(SPOT_ID, approveRequest(), REVIEWER_ID);

            then(storageService).should()
                .copy(eq(PRIVATE_KEY), eq("prod/public/spots/7/original/202607/abc.jpg"));
            then(storageService).should().delete(PRIVATE_KEY);
            then(storageService).should()
                .copy(eq(PRIVATE_THUMB), eq("prod/public/spots/7/thumbnail/202607/abc.jpg"));
            assertThat(image.getImageKey()).isEqualTo("prod/public/spots/7/original/202607/abc.jpg");
            assertThat(image.getThumbnailKey()).isEqualTo("prod/public/spots/7/thumbnail/202607/abc.jpg");
        }
    }

    @Nested
    @DisplayName("반려")
    class Reject {

        @Test
        @DisplayName("사유와 함께 반려하면 REJECTED 로 전이되고 반려 이력을 저장한다")
        void rejectWithReason() {
            Spot spot = buildSpot(SpotStatus.PENDING);
            given(spotRepository.findById(SPOT_ID)).willReturn(Optional.of(spot));

            SpotReviewRequest request =
                new SpotReviewRequest(ReviewDecision.REJECTED, RejectionReason.LOW_QUALITY, null);
            spotReviewService.review(SPOT_ID, request, REVIEWER_ID);

            assertThat(spot.getStatus()).isEqualTo(SpotStatus.REJECTED);

            ArgumentCaptor<SpotReview> captor = ArgumentCaptor.forClass(SpotReview.class);
            then(spotReviewRepository).should().save(captor.capture());
            assertThat(captor.getValue().getReason()).isEqualTo(RejectionReason.LOW_QUALITY);
            then(storageService).should(never()).copy(anyString(), anyString());
        }

        @Test
        @DisplayName("반려 사유가 없으면 SPOT_REJECTION_REASON_REQUIRED 예외를 던진다")
        void throwsWhenReasonMissing() {
            Spot spot = buildSpot(SpotStatus.PENDING);
            given(spotRepository.findById(SPOT_ID)).willReturn(Optional.of(spot));

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
            given(spotRepository.findById(SPOT_ID)).willReturn(Optional.of(spot));

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
        @DisplayName("이미 처리(PUBLISHED)된 스팟을 다시 처리하면 SPOT_ALREADY_REVIEWED 예외를 던진다")
        void throwsWhenAlreadyReviewed() {
            Spot spot = buildSpot(SpotStatus.PUBLISHED);
            given(spotRepository.findById(SPOT_ID)).willReturn(Optional.of(spot));

            assertThatThrownBy(() -> spotReviewService.review(SPOT_ID, approveRequest(), REVIEWER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(SpotErrorCode.SPOT_ALREADY_REVIEWED));

            then(spotReviewRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 스팟이면 SPOT_NOT_FOUND 예외를 던진다")
        void throwsWhenSpotNotFound() {
            given(spotRepository.findById(SPOT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> spotReviewService.review(SPOT_ID, approveRequest(), REVIEWER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(SpotErrorCode.SPOT_NOT_FOUND));
        }
    }

    private static SpotReviewRequest approveRequest() {
        return new SpotReviewRequest(ReviewDecision.APPROVED, null, null);
    }

    private static Spot buildSpot(SpotStatus status) {
        Spot spot = Spot.builder()
            .name("테스트스팟")
            .theme(SpotTheme.SUNSET)
            .latitude(37.5)
            .longitude(127.0)
            .status(status)
            .userId(1L)
            .build();
        ReflectionTestUtils.setField(spot, "id", SPOT_ID);
        return spot;
    }
}
