package com.ioes.photo.domain.spot.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.ioes.photo.domain.spot.enums.SpotOpenRequestStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link SpotOpenRequest} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("SpotOpenRequest 단위 테스트")
class SpotOpenRequestTest {

    private static final Long SPOT_ID = 7L;
    private static final Long USER_ID = 1L;
    private static final LocalDateTime REQUESTED_AT = LocalDateTime.of(2026, 8, 1, 10, 0);
    private static final LocalDateTime RESOLVED_AT = LocalDateTime.of(2026, 8, 2, 15, 30);

    @Test
    @DisplayName("신청 시 진행 중(REQUESTED) 상태로 생성되고 마감 정보는 비어 있다")
    void createsInFlightRequest() {
        SpotOpenRequest request = SpotOpenRequest.request(SPOT_ID, USER_ID, REQUESTED_AT);

        assertThat(request.getSpotId()).isEqualTo(SPOT_ID);
        assertThat(request.getUserId()).isEqualTo(USER_ID);
        assertThat(request.getStatus()).isEqualTo(SpotOpenRequestStatus.REQUESTED);
        assertThat(request.getRequestedAt()).isEqualTo(REQUESTED_AT);
        assertThat(request.getResolvedAt()).isNull();
        assertThat(request.getSpotReviewId()).isNull();
        assertThat(request.isInFlight()).isTrue();
    }

    @Nested
    @DisplayName("검수 마감")
    class ResolveByReview {

        @Test
        @DisplayName("승인이면 APPROVED 로 마감하고 검수 이력과 연결한다")
        void resolvesAsApproved() {
            SpotOpenRequest request = SpotOpenRequest.request(SPOT_ID, USER_ID, REQUESTED_AT);

            request.resolveByReview(true, 55L, RESOLVED_AT);

            assertThat(request.getStatus()).isEqualTo(SpotOpenRequestStatus.APPROVED);
            assertThat(request.getSpotReviewId()).isEqualTo(55L);
            assertThat(request.getResolvedAt()).isEqualTo(RESOLVED_AT);
            assertThat(request.isInFlight()).isFalse();
        }

        @Test
        @DisplayName("반려면 REJECTED 로 마감한다")
        void resolvesAsRejected() {
            SpotOpenRequest request = SpotOpenRequest.request(SPOT_ID, USER_ID, REQUESTED_AT);

            request.resolveByReview(false, 56L, RESOLVED_AT);

            assertThat(request.getStatus()).isEqualTo(SpotOpenRequestStatus.REJECTED);
            assertThat(request.getSpotReviewId()).isEqualTo(56L);
            assertThat(request.isInFlight()).isFalse();
        }
    }

    @Test
    @DisplayName("철회하면 CANCELED 로 마감되고 검수 이력은 연결되지 않는다")
    void cancelsRequest() {
        SpotOpenRequest request = SpotOpenRequest.request(SPOT_ID, USER_ID, REQUESTED_AT);

        request.cancel(RESOLVED_AT);

        assertThat(request.getStatus()).isEqualTo(SpotOpenRequestStatus.CANCELED);
        assertThat(request.getResolvedAt()).isEqualTo(RESOLVED_AT);
        assertThat(request.getSpotReviewId()).isNull();
        assertThat(request.isInFlight()).isFalse();
    }

    @Test
    @DisplayName("신청 시각은 마감 시각과 별개로 보존된다")
    void keepsRequestedAtAfterResolution() {
        SpotOpenRequest request = SpotOpenRequest.request(SPOT_ID, USER_ID, REQUESTED_AT);

        request.resolveByReview(true, 55L, RESOLVED_AT);

        assertThat(request.getRequestedAt()).isEqualTo(REQUESTED_AT);
    }
}
