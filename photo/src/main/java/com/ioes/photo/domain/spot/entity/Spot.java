package com.ioes.photo.domain.spot.entity;

import com.ioes.photo.domain.spot.enums.RelYn;
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
        @Index(name = "idx_spots_region_id", columnList = "region_id"),
        @Index(name = "idx_spots_rel_yn", columnList = "rel_yn"),
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

    // 검수 flow(status)와 별개로 지도뷰/리스트 노출만 껐다 켤 수 있는 독립 플래그.
    @Column(name = "rel_yn", nullable = false, length = 1)
    private RelYn relYn;

    @Column(name = "grid_nx")
    private Integer gridNx;

    @Column(name = "grid_ny")
    private Integer gridNy;

    @Column(name = "crowd_area_name", length = 50)
    private String crowdAreaName;

    @Column(name = "region_id")
    private Long regionId;

    @Column(name = "parking_info", length = 255)
    private String parkingInfo;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "bookmark_count", nullable = false)
    private long bookmarkCount = 0;

    @Column(name = "like_count", nullable = false)
    private long likeCount = 0;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private Spot(String name, String comment, SpotTheme theme, Double latitude, Double longitude,
                 String address, String addressRoad, String addressJibun, SpotStatus status,
                 Integer gridNx, Integer gridNy, String crowdAreaName, Long regionId,
                 String parkingInfo, Long userId) {
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
        // 검수 없이 곧바로 PUBLISHED 로 생성되는 어드민 큐레이션/배치 등록 스팟은 처음부터 노출 상태다.
        this.relYn = this.status == SpotStatus.PUBLISHED ? RelYn.Y : RelYn.N;
        this.gridNx = gridNx;
        this.gridNy = gridNy;
        this.crowdAreaName = crowdAreaName;
        this.regionId = regionId;
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

    public void assignRegion(Long regionId) {
        this.regionId = regionId;
    }

    // userId 가 없는 건은 운영자가 등록한 큐레이션 스팟이다.
    public boolean isCurated() {
        return userId == null;
    }

    public boolean isOwnedBy(Long candidateUserId) {
        return candidateUserId != null && candidateUserId.equals(userId);
    }

    public boolean isPublished() {
        return status == SpotStatus.PUBLISHED;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    // 관리자 큐레이션은 검수 절차를 거치지 않으므로 상태와 무관하게 허용한다.
    public boolean isLikeable() {
        return isPublished() || isCurated();
    }

    public boolean isOpenRequestable() {
        return status == SpotStatus.DRAFT || status == SpotStatus.REJECTED;
    }

    public boolean isReviewable() {
        return status == SpotStatus.PENDING || status == SpotStatus.RE_REVIEW_PENDING;
    }

    // 검수 대기 중이면 '오픈 신청 철회', 공개 상태면 '비공개 전환'에 해당한다.
    public boolean isPublicationCancelable() {
        return isReviewable() || isPublished();
    }

    public void requestOpen(LocalDateTime requestedAt) {
        this.status = status == SpotStatus.REJECTED ? SpotStatus.RE_REVIEW_PENDING : SpotStatus.PENDING;
        this.appliedAt = requestedAt;
    }

    public void applyReview(boolean approved, Long reviewerId, LocalDateTime reviewedAt) {
        this.status = approved ? SpotStatus.PUBLISHED : SpotStatus.REJECTED;
        if (approved) {
            this.relYn = RelYn.Y;
        }
        this.reviewerId = reviewerId;
        this.reviewedAt = reviewedAt;
    }

    // appliedAt 은 검수 큐 정렬 기준이라 비우고, reviewedAt/reviewerId 는 마지막 검수 사실 기록이라 남긴다.
    // relYn 도 함께 초기화한다. 그대로 두면 재승인 전까지 노출 여부를 알 수 없는 상태로 남는다.
    public void cancelPublication() {
        this.status = SpotStatus.DRAFT;
        this.appliedAt = null;
        this.relYn = RelYn.N;
    }

    public boolean isReleased() {
        return relYn == RelYn.Y;
    }

    public boolean isReleaseControllable() {
        return status == SpotStatus.PUBLISHED;
    }

    public void release() {
        this.relYn = RelYn.Y;
    }

    public void unrelease() {
        this.relYn = RelYn.N;
    }

    // 검수 중 수정은 운영자가 확인한 것과 다른 내용을 승인하게 만들고, 공개 상태 수정은 재검수를 우회한다.
    public boolean isEditable() {
        return status == SpotStatus.DRAFT || status == SpotStatus.REJECTED;
    }

    // 검수 큐에 올라간 건이 사라지면 운영자 화면이 깨진다. 철회 후 삭제하는 흐름으로 유도한다.
    public boolean isDeletable() {
        return !isReviewable();
    }

    public void updateBasic(String name, String comment, SpotTheme theme) {
        this.name = name;
        this.comment = comment;
        this.theme = theme;
    }

    public void updateParkingInfo(String parkingInfo) {
        this.parkingInfo = parkingInfo;
    }

    public void updateLocation(Double latitude, Double longitude,
                               String address, String addressRoad, String addressJibun) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.location = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        this.address = address;
        this.addressRoad = addressRoad;
        this.addressJibun = addressJibun;
    }

    public boolean isAt(Double latitude, Double longitude) {
        return this.latitude.equals(latitude) && this.longitude.equals(longitude);
    }

    public void softDelete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
