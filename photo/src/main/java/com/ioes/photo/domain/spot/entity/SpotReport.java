package com.ioes.photo.domain.spot.entity;

import com.ioes.photo.domain.spot.enums.SpotReportStatus;
import com.ioes.photo.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 스팟 잘못된 정보 신고 엔티티.
 *
 * @author 황제연
 */
@Getter
@Entity
@Table(name = "spot_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotReport extends BaseEntity {

    @Column(name = "spot_id", nullable = false)
    private Long spotId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 1)
    private SpotReportStatus status;

    @Column(nullable = false, length = 5000)
    private String content;

    @Builder
    private SpotReport(Long spotId, Long userId, String content) {
        this.spotId = spotId;
        this.userId = userId;
        this.status = SpotReportStatus.PENDING;
        this.content = content;
    }
}
