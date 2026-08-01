package com.ioes.photo.domain.spot.entity;

import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
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
import org.hibernate.annotations.SQLRestriction;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

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
        @Index(name = "idx_spots_user_id", columnList = "user_id"),
    }
)
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Spot extends BaseEntity {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(nullable = false, length = 4)
    private SpotTheme theme;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Column(length = 255)
    private String address;

    @Column(name = "address_road", length = 255)
    private String addressRoad;

    @Column(name = "address_jibun", length = 255)
    private String addressJibun;

    @Column(nullable = false, length = 4)
    private SpotStatus status;

    @Column(name = "grid_nx")
    private Integer gridNx;

    @Column(name = "grid_ny")
    private Integer gridNy;

    @Column(name = "crowd_area_name", length = 50)
    private String crowdAreaName;

    @Column(name = "parking_info", length = 255)
    private String parkingInfo;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "bookmark_count", nullable = false)
    private long bookmarkCount = 0;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private Spot(String name, String comment, SpotTheme theme, Double latitude, Double longitude,
                 String address, String addressRoad, String addressJibun, SpotStatus status,
                 Integer gridNx, Integer gridNy, String crowdAreaName, String parkingInfo, Long userId) {
        this.name = name;
        this.comment = comment;
        this.theme = theme;
        this.latitude = latitude;
        this.longitude = longitude;
        this.location = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        this.address = address;
        this.addressRoad = addressRoad;
        this.addressJibun = addressJibun;
        this.status = status == null ? SpotStatus.DRAFT : status;
        this.gridNx = gridNx;
        this.gridNy = gridNy;
        this.crowdAreaName = crowdAreaName;
        this.parkingInfo = parkingInfo;
        this.userId = userId;
    }

    public void assignGrid(Integer gridNx, Integer gridNy) {
        this.gridNx = gridNx;
        this.gridNy = gridNy;
    }

    public void assignCrowdAreaName(String crowdAreaName) {
        this.crowdAreaName = crowdAreaName;
    }

    public boolean isOpenRequestable() {
        return status == SpotStatus.DRAFT || status == SpotStatus.REJECTED;
    }

    public boolean isReviewable() {
        return status == SpotStatus.PENDING || status == SpotStatus.RE_REVIEW_PENDING;
    }

    public void requestOpen(LocalDateTime requestedAt) {
        this.status = status == SpotStatus.REJECTED ? SpotStatus.RE_REVIEW_PENDING : SpotStatus.PENDING;
        this.appliedAt = requestedAt;
    }

    public void applyReview(boolean approved, Long reviewerId, LocalDateTime reviewedAt) {
        this.status = approved ? SpotStatus.PUBLISHED : SpotStatus.REJECTED;
        this.reviewerId = reviewerId;
        this.reviewedAt = reviewedAt;
    }
}
