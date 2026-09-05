package com.ioes.photo.domain.notification.service;

import com.ioes.photo.domain.notification.dto.SpotOpenReviewHistoryCheckResponse;
import com.ioes.photo.domain.notification.dto.SpotOpenReviewHistoryListResponse;
import com.ioes.photo.domain.notification.dto.SpotOpenReviewHistoryListResponse.ApprovedItem;
import com.ioes.photo.domain.notification.dto.SpotOpenReviewHistoryListResponse.RejectedItem;
import com.ioes.photo.domain.notification.entity.SpotOpenReviewHistory;
import com.ioes.photo.domain.notification.enums.CheckYn;
import com.ioes.photo.domain.notification.error.NotificationErrorCode;
import com.ioes.photo.domain.notification.repository.SpotOpenReviewHistoryRepository;
import com.ioes.photo.domain.spot.enums.SpotOpenRequestStatus;
import com.ioes.photo.domain.spot.event.SpotOpenReviewCompletedEvent;
import com.ioes.photo.global.common.util.NullUtils;
import com.ioes.photo.global.error.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 스팟 검수완료 알림 히스토리 서비스.
 *
 * 히스토리 생성은 검수완료 이벤트 리스너를 통해서만 이뤄지며, 별도의 공개 등록 API는 없다.
 *
 * @author 황제연
 */
@Service
@RequiredArgsConstructor
public class SpotOpenReviewHistoryService {

    private final SpotOpenReviewHistoryRepository spotOpenReviewHistoryRepository;

    @Transactional
    public void record(SpotOpenReviewCompletedEvent event) {
        SpotOpenReviewHistory history = SpotOpenReviewHistory.record(
            event.spotId(), event.userId(), event.status(), event.rejectReason(), event.rejectDetail());
        spotOpenReviewHistoryRepository.save(history);
    }

    @Transactional
    public SpotOpenReviewHistoryCheckResponse markChecked(Long userId, Long historyId) {
        SpotOpenReviewHistory history = NullUtils.orElseThrow(
            spotOpenReviewHistoryRepository.findById(historyId),
            () -> new BusinessException(NotificationErrorCode.REVIEW_HISTORY_NOT_FOUND));

        if (!userId.equals(history.getUserId())) {
            throw new BusinessException(NotificationErrorCode.REVIEW_HISTORY_ACCESS_DENIED);
        }

        history.markChecked();
        return SpotOpenReviewHistoryCheckResponse.from(history);
    }

    @Transactional(readOnly = true)
    public SpotOpenReviewHistoryListResponse findUnchecked(Long userId) {
        List<SpotOpenReviewHistory> histories =
            spotOpenReviewHistoryRepository.findByUserIdAndCheckYnOrderByCreatedAtDesc(userId, CheckYn.N);

        List<ApprovedItem> approved = histories.stream()
            .filter(history -> history.getSpotStatus() == SpotOpenRequestStatus.APPROVED)
            .map(ApprovedItem::from)
            .toList();

        List<RejectedItem> rejected = histories.stream()
            .filter(history -> history.getSpotStatus() == SpotOpenRequestStatus.REJECTED)
            .map(RejectedItem::from)
            .toList();

        return new SpotOpenReviewHistoryListResponse(approved, rejected);
    }
}
