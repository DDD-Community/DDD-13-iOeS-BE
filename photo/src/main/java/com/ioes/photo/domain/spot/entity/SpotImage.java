package com.ioes.photo.domain.spot.entity;

import com.ioes.photo.domain.spot.enums.ImageSourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 스팟 대표 이미지.
 *
 * spots와 1:1 관계를 공유 PK(spot_id)로 표현한다.
 * imageSourceType이 INTERNAL이면 image_key는 자사 S3/MinIO 객체 키이며 URL은 조회 시점에 동적 생성한다.
 * EXTERNAL이면 image_key에 외부 호스팅 URL을 그대로 저장하며 조회 시에도 그 URL을 그대로 반환한다(hotlink).
 *
 * @author 황제연
 */
@Getter
@Entity
@Table(name = "spot_images")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotImage {

    @Id
    @Column(name = "spot_id")
    private Long spotId;

    @Column(name = "image_key", nullable = false)
    private String imageKey;

    @Column(name = "thumbnail_key")
    private String thumbnailKey;

    @Column(name = "image_source_type", nullable = false)
    private ImageSourceType imageSourceType;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "recorded_date")
    private LocalDate recordedDate;

    @Column(name = "recorded_time")
    private LocalTime recordedTime;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private SpotImage(Long spotId, String imageKey, String originalFilename, String contentType,
                       ImageSourceType imageSourceType) {
        this.spotId = spotId;
        this.imageKey = imageKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.imageSourceType = imageSourceType;
    }

    public static SpotImage create(Long spotId, String imageKey) {
        return new SpotImage(spotId, imageKey, null, null, ImageSourceType.INTERNAL);
    }

    public static SpotImage create(Long spotId, String imageKey, String originalFilename, String contentType) {
        return new SpotImage(spotId, imageKey, originalFilename, contentType, ImageSourceType.INTERNAL);
    }

    // 외부 호스팅 이미지 연동(hotlink). 자사 스토리지 업로드나 썸네일 생성을 하지 않는다.
    public static SpotImage createExternal(Long spotId, String externalUrl) {
        return new SpotImage(spotId, externalUrl, null, null, ImageSourceType.EXTERNAL);
    }

    public boolean isExternal() {
        return imageSourceType == ImageSourceType.EXTERNAL;
    }

    public void updateImageKey(String imageKey) {
        this.imageKey = imageKey;
    }

    public void updateImageSourceType(ImageSourceType imageSourceType) {
        this.imageSourceType = imageSourceType;
    }

    public void updateThumbnailKey(String thumbnailKey) {
        this.thumbnailKey = thumbnailKey;
    }

    public void updateOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public void updateContentType(String contentType) {
        this.contentType = contentType;
    }

    public void updateRecordedTime(LocalTime recordedTime) {
        this.recordedTime = recordedTime;
    }

    public void updateRecordedDate(LocalDate recordedDate) {
        this.recordedDate = recordedDate;
    }
}
