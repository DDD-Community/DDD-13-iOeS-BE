package com.ioes.photo.domain.notification.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ioes.photo.domain.notification.enums.CheckYn;
import com.ioes.photo.domain.notification.error.NotificationErrorCode;
import com.ioes.photo.domain.spot.enums.RejectionReason;
import com.ioes.photo.domain.spot.enums.SpotOpenRequestStatus;
import com.ioes.photo.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link SpotOpenReviewHistory} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("SpotOpenReviewHistory 단위 테스트")
class SpotOpenReviewHistoryTest {

    private static final Long SPOT_ID = 1L;
    private static final Long USER_ID = 10L;

    @Test
    @DisplayName("승인 히스토리를 생성하면 확인여부는 N이고 반려 사유는 항상 null이다")
    void recordApproved() {
        SpotOpenReviewHistory history = SpotOpenReviewHistory.record(
            SPOT_ID, USER_ID, SpotOpenRequestStatus.APPROVED, RejectionReason.DUPLICATE, "무시되어야 함");

        assertThat(history.getSpotId()).isEqualTo(SPOT_ID);
        assertThat(history.getUserId()).isEqualTo(USER_ID);
        assertThat(history.getSpotStatus()).isEqualTo(SpotOpenRequestStatus.APPROVED);
        assertThat(history.getRejectReason()).isNull();
        assertThat(history.getRejectDetail()).isNull();
        assertThat(history.getCheckYn()).isEqualTo(CheckYn.N);
    }

    @Test
    @DisplayName("반려 히스토리를 생성하면 반려 사유와 상세가 그대로 저장된다")
    void recordRejected() {
        SpotOpenReviewHistory history = SpotOpenReviewHistory.record(
            SPOT_ID, USER_ID, SpotOpenRequestStatus.REJECTED, RejectionReason.LOW_QUALITY, "화질이 낮아요");

        assertThat(history.getSpotStatus()).isEqualTo(SpotOpenRequestStatus.REJECTED);
        assertThat(history.getRejectReason()).isEqualTo(RejectionReason.LOW_QUALITY);
        assertThat(history.getRejectDetail()).isEqualTo("화질이 낮아요");
        assertThat(history.getCheckYn()).isEqualTo(CheckYn.N);
    }

    @Test
    @DisplayName("반려 히스토리인데 반려 사유가 없으면 도메인 예외를 던진다")
    void recordRejectedWithoutReasonThrows() {
        assertThatThrownBy(() ->
            SpotOpenReviewHistory.record(SPOT_ID, USER_ID, SpotOpenRequestStatus.REJECTED, null, null))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                .isEqualTo(NotificationErrorCode.REVIEW_HISTORY_REJECT_REASON_REQUIRED));
    }

    @Test
    @DisplayName("APPROVED/REJECTED 외 상태로 생성하려 하면 IllegalArgumentException을 던진다")
    void recordWithInvalidStatusThrows() {
        assertThatThrownBy(() ->
            SpotOpenReviewHistory.record(SPOT_ID, USER_ID, SpotOpenRequestStatus.REQUESTED, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("markChecked 호출 시 확인여부가 Y로 전환되며, 여러 번 호출해도 안전하다")
    void markCheckedIsIdempotent() {
        SpotOpenReviewHistory history = SpotOpenReviewHistory.record(
            SPOT_ID, USER_ID, SpotOpenRequestStatus.APPROVED, null, null);

        history.markChecked();
        history.markChecked();

        assertThat(history.getCheckYn()).isEqualTo(CheckYn.Y);
    }
}
