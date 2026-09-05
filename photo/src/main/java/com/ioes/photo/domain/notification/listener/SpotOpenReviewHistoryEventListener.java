package com.ioes.photo.domain.notification.listener;

import com.ioes.photo.domain.notification.service.SpotOpenReviewHistoryService;
import com.ioes.photo.domain.spot.event.SpotOpenReviewCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 스팟 검수완료 이벤트 수신 리스너.
 *
 * 검수 트랜잭션이 커밋된 이후에만 히스토리를 적재해, 검수 자체가 롤백되면 히스토리도 함께 남지 않도록 한다.
 * {@code @Async} 로 별도 스레드에서 처리해, AFTER_COMMIT 시점에 원본 트랜잭션의 리소스가 아직
 * 완전히 해제되지 않아 새 저장 트랜잭션이 이를 그대로 이어받는 문제(SpotCreatedListener와 동일한 패턴)를 피한다.
 * 히스토리 적재 실패가 검수 처리 결과에 영향을 주지 않도록 예외를 흡수하고 로그만 남긴다.
 * 운영자가 직접 등록한 큐레이션 스팟(userId 없음)은 알림 대상 소유자가 없어 조용히 건너뛴다.
 *
 * @author 황제연
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpotOpenReviewHistoryEventListener {

    private final SpotOpenReviewHistoryService spotOpenReviewHistoryService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSpotOpenReviewCompleted(SpotOpenReviewCompletedEvent event) {
        if (event.userId() == null) {
            log.debug("[SpotOpenReviewHistoryEventListener] 소유자 없는 큐레이션 스팟이라 히스토리를 건너뜁니다. spotId={}",
                event.spotId());
            return;
        }

        try {
            spotOpenReviewHistoryService.record(event);
        } catch (Exception e) {
            log.error("[SpotOpenReviewHistoryEventListener] 검수완료 히스토리 적재 실패 spotId={} userId={} status={}",
                event.spotId(), event.userId(), event.status(), e);
        }
    }
}
