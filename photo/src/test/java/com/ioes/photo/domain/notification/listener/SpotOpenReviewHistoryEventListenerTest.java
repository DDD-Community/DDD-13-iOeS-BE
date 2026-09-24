package com.ioes.photo.domain.notification.listener;

import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.ioes.photo.domain.notification.service.SpotOpenReviewHistoryService;
import com.ioes.photo.domain.spot.enums.SpotOpenRequestStatus;
import com.ioes.photo.domain.spot.event.SpotOpenReviewCompletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link SpotOpenReviewHistoryEventListener} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpotOpenReviewHistoryEventListener 단위 테스트")
class SpotOpenReviewHistoryEventListenerTest {

    @Mock SpotOpenReviewHistoryService spotOpenReviewHistoryService;

    @InjectMocks SpotOpenReviewHistoryEventListener listener;

    @Test
    @DisplayName("userId가 있으면 히스토리 저장을 위임한다")
    void delegatesRecordWhenUserIdPresent() {
        SpotOpenReviewCompletedEvent event =
            new SpotOpenReviewCompletedEvent(1L, 10L, SpotOpenRequestStatus.APPROVED, null, null);

        listener.onSpotOpenReviewCompleted(event);

        then(spotOpenReviewHistoryService).should().record(event);
    }

    @Test
    @DisplayName("userId가 없으면(큐레이션 스팟) 히스토리 저장을 건너뛴다")
    void skipsWhenUserIdMissing() {
        SpotOpenReviewCompletedEvent event =
            new SpotOpenReviewCompletedEvent(1L, null, SpotOpenRequestStatus.APPROVED, null, null);

        listener.onSpotOpenReviewCompleted(event);

        then(spotOpenReviewHistoryService).should(never()).record(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("히스토리 저장 중 예외가 발생해도 리스너 밖으로 전파되지 않는다")
    void swallowsExceptionFromService() {
        SpotOpenReviewCompletedEvent event =
            new SpotOpenReviewCompletedEvent(1L, 10L, SpotOpenRequestStatus.REJECTED, null, null);
        willThrow(new RuntimeException("db down")).given(spotOpenReviewHistoryService).record(event);

        listener.onSpotOpenReviewCompleted(event);

        then(spotOpenReviewHistoryService).should().record(event);
    }
}
