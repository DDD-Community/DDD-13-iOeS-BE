package com.ioes.photo.domain.spot.event;

import com.ioes.photo.domain.spot.enums.RejectionReason;
import com.ioes.photo.domain.spot.enums.SpotOpenRequestStatus;

/**
 * 스팟 오픈 신청 검수완료(승인/반려) 이벤트.
 *
 * 검수 트랜잭션 커밋 이후 사용자 알림 히스토리 적재를 트리거하기 위해 발행한다.
 * userId 가 없는 건은 운영자가 직접 등록한 큐레이션 스팟으로, 히스토리를 남기지 않는다.
 *
 * @author 황제연
 */
public record SpotOpenReviewCompletedEvent(
    Long spotId,
    Long userId,
    SpotOpenRequestStatus status,
    RejectionReason rejectReason,
    String rejectDetail
) {
}
