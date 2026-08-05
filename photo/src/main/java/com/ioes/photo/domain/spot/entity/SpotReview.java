package com.ioes.photo.domain.spot.entity;

import com.ioes.photo.domain.spot.enums.RejectionReason;
import com.ioes.photo.domain.spot.enums.ReviewDecision;
import com.ioes.photo.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 스팟 검수 이력.
 *
 * 운영자가 오픈 신청 건을 승인/반려할 때마다 1행이 적재되는 append-only 이력이다.
 * 반려 이력(REJECTED)은 재검토 화면에서 이전 반려 사유를 비교하는 데 사용된다.
 * spots 와는 연관관계 매핑 없이 spotId(PK) 로 연결한다.
 *
 * @author 황제연
 */
@Getter
@Entity
@Table(
    name = "spot_reviews",
    indexes = {
        @Index(name = "idx_spot_reviews_spot_id", columnList = "spot_id"),
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotReview extends BaseEntity {

    @Column(name = "spot_id", nullable = false)
    private Long spotId;

    @Column(nullable = false, length = 4)
    private ReviewDecision decision;

    @Column(length = 4)
    private RejectionReason reason;

    @Column(columnDefinition = "text")
    private String detail;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Builder(access = AccessLevel.PRIVATE)
    private SpotReview(Long spotId, ReviewDecision decision, RejectionReason reason,
                       String detail, Long reviewerId) {
        this.spotId = spotId;
        this.decision = decision;
        this.reason = reason;
        this.detail = detail;
        this.reviewerId = reviewerId;
    }

    public static SpotReview approved(Long spotId, Long reviewerId) {
        return SpotReview.builder()
            .spotId(spotId)
            .decision(ReviewDecision.APPROVED)
            .reviewerId(reviewerId)
            .build();
    }

    public static SpotReview rejected(Long spotId, Long reviewerId, RejectionReason reason, String detail) {
        return SpotReview.builder()
            .spotId(spotId)
            .decision(ReviewDecision.REJECTED)
            .reason(reason)
            .detail(detail)
            .reviewerId(reviewerId)
            .build();
    }
}
