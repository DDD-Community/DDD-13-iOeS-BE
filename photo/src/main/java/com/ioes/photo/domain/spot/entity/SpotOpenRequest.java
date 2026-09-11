package com.ioes.photo.domain.spot.entity;

import com.ioes.photo.domain.spot.enums.SpotOpenRequestStatus;
import com.ioes.photo.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 스팟 오픈 신청 이력.
 *
 * 사용자가 '오픈하기'를 누를 때마다 1행이 적재된다. 반려 후 재신청도 새 행으로 쌓여,
 * 한 스팟이 몇 번 신청되고 어떻게 마감됐는지가 그대로 남는다.
 * spots 와는 연관관계 매핑 없이 spotId(PK) 로 연결한다.
 *
 * @author 황제연
 */
@Getter
@Entity
@Table(
    name = "spot_open_requests",
    indexes = {
        @Index(name = "idx_spot_open_requests_spot_id", columnList = "spot_id"),
        @Index(name = "idx_spot_open_requests_user_id", columnList = "user_id"),
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotOpenRequest extends BaseEntity {

    @Column(name = "spot_id", nullable = false)
    private Long spotId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 4)
    private SpotOpenRequestStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "spot_review_id")
    private Long spotReviewId;

    @Builder(access = AccessLevel.PRIVATE)
    private SpotOpenRequest(Long spotId, Long userId, SpotOpenRequestStatus status,
                            LocalDateTime requestedAt) {
        this.spotId = spotId;
        this.userId = userId;
        this.status = status;
        this.requestedAt = requestedAt;
    }

    public static SpotOpenRequest request(Long spotId, Long userId, LocalDateTime requestedAt) {
        return SpotOpenRequest.builder()
            .spotId(spotId)
            .userId(userId)
            .status(SpotOpenRequestStatus.REQUESTED)
            .requestedAt(requestedAt)
            .build();
    }

    public boolean isInFlight() {
        return status == SpotOpenRequestStatus.REQUESTED;
    }

    public void resolveByReview(boolean approved, Long spotReviewId, LocalDateTime resolvedAt) {
        this.status = approved ? SpotOpenRequestStatus.APPROVED : SpotOpenRequestStatus.REJECTED;
        this.spotReviewId = spotReviewId;
        this.resolvedAt = resolvedAt;
    }

    public void cancel(LocalDateTime resolvedAt) {
        this.status = SpotOpenRequestStatus.CANCELED;
        this.resolvedAt = resolvedAt;
    }
}
