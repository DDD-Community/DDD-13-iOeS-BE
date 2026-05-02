package com.ioes.photo.domain.spot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
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
 * DB에는 S3 객체 키만 저장하며 URL은 조회 시점에 동적 생성한다.
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

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "content_type")
    private String contentType;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private SpotImage(Long spotId, String imageKey, String originalFilename, String contentType) {
        this.spotId = spotId;
        this.imageKey = imageKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
    }

    public static SpotImage create(Long spotId, String imageKey) {
        return new SpotImage(spotId, imageKey, null, null);
    }

    public static SpotImage create(Long spotId, String imageKey, String originalFilename, String contentType) {
        return new SpotImage(spotId, imageKey, originalFilename, contentType);
    }

    public void updateImageKey(String imageKey) {
        this.imageKey = imageKey;
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
}
