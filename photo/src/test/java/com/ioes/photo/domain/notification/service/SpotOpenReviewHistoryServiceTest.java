package com.ioes.photo.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ioes.photo.domain.notification.dto.SpotOpenReviewHistoryCheckResponse;
import com.ioes.photo.domain.notification.dto.SpotOpenReviewHistoryListResponse;
import com.ioes.photo.domain.notification.entity.SpotOpenReviewHistory;
import com.ioes.photo.domain.notification.enums.CheckYn;
import com.ioes.photo.domain.notification.error.NotificationErrorCode;
import com.ioes.photo.domain.notification.repository.SpotOpenReviewHistoryRepository;
import com.ioes.photo.domain.spot.enums.RejectionReason;
import com.ioes.photo.domain.spot.enums.SpotOpenRequestStatus;
import com.ioes.photo.domain.spot.event.SpotOpenReviewCompletedEvent;
import com.ioes.photo.global.error.exception.BusinessException;
import java.util.List;
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
 * {@link SpotOpenReviewHistoryService} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpotOpenReviewHistoryService 단위 테스트")
class SpotOpenReviewHistoryServiceTest {

    @Mock SpotOpenReviewHistoryRepository spotOpenReviewHistoryRepository;

    @InjectMocks SpotOpenReviewHistoryService spotOpenReviewHistoryService;

    private static final Long SPOT_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final Long HISTORY_ID = 100L;

    @Nested
    @DisplayName("record")
    class Record {

        @Test
        @DisplayName("이벤트를 받아 히스토리를 생성하고 저장한다")
        void savesHistoryFromEvent() {
            SpotOpenReviewCompletedEvent event = new SpotOpenReviewCompletedEvent(
                SPOT_ID, USER_ID, SpotOpenRequestStatus.APPROVED, null, null);

            spotOpenReviewHistoryService.record(event);

            ArgumentCaptor<SpotOpenReviewHistory> captor = ArgumentCaptor.forClass(SpotOpenReviewHistory.class);
            then(spotOpenReviewHistoryRepository).should().save(captor.capture());
            assertThat(captor.getValue().getSpotId()).isEqualTo(SPOT_ID);
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("markChecked")
    class MarkChecked {

        @Test
        @DisplayName("본인 소유 히스토리를 확인 처리하면 checkYn이 Y로 응답된다")
        void marksOwnedHistoryChecked() {
            SpotOpenReviewHistory history = buildHistory(SpotOpenRequestStatus.APPROVED, null, null);
            given(spotOpenReviewHistoryRepository.findById(HISTORY_ID)).willReturn(Optional.of(history));

            SpotOpenReviewHistoryCheckResponse response =
                spotOpenReviewHistoryService.markChecked(USER_ID, HISTORY_ID);

            assertThat(response.historyId()).isEqualTo(HISTORY_ID);
            assertThat(response.checkYn()).isEqualTo("Y");
        }

        @Test
        @DisplayName("존재하지 않는 히스토리면 REVIEW_HISTORY_NOT_FOUND 예외를 던진다")
        void throwsWhenNotFound() {
            given(spotOpenReviewHistoryRepository.findById(HISTORY_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> spotOpenReviewHistoryService.markChecked(USER_ID, HISTORY_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(NotificationErrorCode.REVIEW_HISTORY_NOT_FOUND));
        }

        @Test
        @DisplayName("본인 소유가 아니면 REVIEW_HISTORY_ACCESS_DENIED 예외를 던진다")
        void throwsWhenNotOwner() {
            SpotOpenReviewHistory history = buildHistory(SpotOpenRequestStatus.APPROVED, null, null);
            given(spotOpenReviewHistoryRepository.findById(HISTORY_ID)).willReturn(Optional.of(history));

            assertThatThrownBy(() -> spotOpenReviewHistoryService.markChecked(999L, HISTORY_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(NotificationErrorCode.REVIEW_HISTORY_ACCESS_DENIED));
        }

        @Test
        @DisplayName("이미 확인된 히스토리를 다시 확인 처리해도 에러 없이 성공한다")
        void idempotentWhenAlreadyChecked() {
            SpotOpenReviewHistory history = buildHistory(SpotOpenRequestStatus.APPROVED, null, null);
            history.markChecked();
            given(spotOpenReviewHistoryRepository.findById(HISTORY_ID)).willReturn(Optional.of(history));

            SpotOpenReviewHistoryCheckResponse response =
                spotOpenReviewHistoryService.markChecked(USER_ID, HISTORY_ID);

            assertThat(response.checkYn()).isEqualTo("Y");
        }
    }

    @Nested
    @DisplayName("findUnchecked")
    class FindUnchecked {

        @Test
        @DisplayName("미확인 히스토리를 승인/반려 목록으로 분리해서 반환한다")
        void splitsApprovedAndRejected() {
            SpotOpenReviewHistory approved = buildHistory(SpotOpenRequestStatus.APPROVED, null, null);
            SpotOpenReviewHistory rejected =
                buildHistory(SpotOpenRequestStatus.REJECTED, RejectionReason.LOW_QUALITY, "화질 불량");
            given(spotOpenReviewHistoryRepository.findByUserIdAndCheckYnOrderByCreatedAtDesc(USER_ID, CheckYn.N))
                .willReturn(List.of(rejected, approved));

            SpotOpenReviewHistoryListResponse response = spotOpenReviewHistoryService.findUnchecked(USER_ID);

            assertThat(response.approved()).hasSize(1);
            assertThat(response.approved().get(0).historyId()).isEqualTo(HISTORY_ID);
            assertThat(response.rejected()).hasSize(1);
            assertThat(response.rejected().get(0).rejectReason()).isEqualTo("LOW_QUALITY");
            assertThat(response.rejected().get(0).rejectReasonLabel()).isEqualTo("사진 상태 불량");
        }
    }

    private SpotOpenReviewHistory buildHistory(SpotOpenRequestStatus status, RejectionReason reason, String detail) {
        SpotOpenReviewHistory history = SpotOpenReviewHistory.record(SPOT_ID, USER_ID, status, reason, detail);
        ReflectionTestUtils.setField(history, "id", HISTORY_ID);
        return history;
    }
}
