package com.ioes.photo.domain.spot.entity;

import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * 사진 촬영 스팟 엔티티.
 *
 * 외부 API 스케줄러가 대상 지역을 조회하기 위해 {@code gridNx/Ny}(기상청 격자) 및
 * {@code crowdAreaCd}(서울시 혼잡도 지역코드)를 보관한다.
 *
 * @author 김성민
 */
@Getter
@Entity
@Table(
    name = "spots",
    indexes = {
        @Index(name = "idx_spots_theme", columnList = "theme"),
        @Index(name = "idx_spots_status", columnList = "status"),
        @Index(name = "idx_spots_crowd_area_name", columnList = "crowd_area_name"),
        @Index(name = "idx_spots_lat_lng", columnList = "latitude, longitude")
    }
)
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Spot extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SpotTheme theme;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SpotStatus status;

    @Column(name = "grid_nx")
    private Integer gridNx;

    @Column(name = "grid_ny")
    private Integer gridNy;

    @Column(name = "crowd_area_name", length = 50)
    private String crowdAreaName;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private Spot(String name, String comment, SpotTheme theme, Double latitude, Double longitude,
                 String address, SpotStatus status, Integer gridNx, Integer gridNy, String crowdAreaName) {
        this.name = name;
        this.comment = comment;
        this.theme = theme;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.status = status == null ? SpotStatus.PENDING : status;
        this.gridNx = gridNx;
        this.gridNy = gridNy;
        this.crowdAreaName = crowdAreaName;
    }
}
