package com.ioes.photo.domain.notification.entity;

import com.ioes.photo.domain.notification.enums.CheckYn;
import com.ioes.photo.domain.notification.error.NotificationErrorCode;
import com.ioes.photo.domain.spot.enums.RejectionReason;
import com.ioes.photo.domain.spot.enums.SpotOpenRequestStatus;
import com.ioes.photo.global.entity.BaseEntity;
import com.ioes.photo.global.error.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 스팟 검수완료 알림 히스토리.
 *
 * 운영자가 오픈 신청 건을 승인/반려할 때마다 이벤트 기반으로 1행이 적재되는 사용자별 알림 이력이다.
 * 공개 등록 API는 없으며, 오직 검수완료 이벤트 리스너를 통해서만 생성된다.
 * spots/spot_open_requests 와는 연관관계 매핑 없이 spotId(PK) 로 연결한다.
 *
 * @author 황제연
 */
@Getter
@Entity
@Table(
    name = "spot_open_review_history",
    indexes = {
        @Index(name = "idx_spot_open_review_history_user_check", columnList = "user_id, check_yn"),
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotOpenReviewHistory extends BaseEntity {

    @Column(name = "spot_id", nullable = false)
    private Long spotId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "spot_status", nullable = false, length = 4)
    private SpotOpenRequestStatus spotStatus;

    @Column(name = "reject_reason", length = 4)
    private RejectionReason rejectReason;

    @Column(name = "reject_detail", columnDefinition = "text")
    private String rejectDetail;

    @Column(name = "check_yn", nullable = false, length = 1)
    private CheckYn checkYn;

    @Builder(access = AccessLevel.PRIVATE)
    private SpotOpenReviewHistory(Long spotId, Long userId, SpotOpenRequestStatus spotStatus,
                                  RejectionReason rejectReason, String rejectDetail, CheckYn checkYn) {
        this.spotId = spotId;
        this.userId = userId;
        this.spotStatus = spotStatus;
        this.rejectReason = rejectReason;
        this.rejectDetail = rejectDetail;
        this.checkYn = checkYn;
    }

    public static SpotOpenReviewHistory record(Long spotId, Long userId, SpotOpenRequestStatus status,
                                               RejectionReason rejectReason, String rejectDetail) {
        if (status != SpotOpenRequestStatus.APPROVED && status != SpotOpenRequestStatus.REJECTED) {
            throw new IllegalArgumentException("검수완료 히스토리는 APPROVED/REJECTED 상태만 가질 수 있습니다: " + status);
        }
        if (status == SpotOpenRequestStatus.REJECTED && rejectReason == null) {
            throw new BusinessException(NotificationErrorCode.REVIEW_HISTORY_REJECT_REASON_REQUIRED);
        }

        boolean approved = status == SpotOpenRequestStatus.APPROVED;
        return SpotOpenReviewHistory.builder()
            .spotId(spotId)
            .userId(userId)
            .spotStatus(status)
            .rejectReason(approved ? null : rejectReason)
            .rejectDetail(approved ? null : rejectDetail)
            .checkYn(CheckYn.N)
            .build();
    }

    public void markChecked() {
        this.checkYn = CheckYn.Y;
    }
}
