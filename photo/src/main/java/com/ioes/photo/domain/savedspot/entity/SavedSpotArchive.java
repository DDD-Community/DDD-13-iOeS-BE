package com.ioes.photo.domain.savedspot.entity;

import com.ioes.photo.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * 사용자가 저장(북마크)한 스팟 아카이브 엔티티.
 *
 * user_id + spot_id 조합에 UNIQUE 제약을 두고, 재북마크 시 deleted_at을 초기화하는 재활성화 방식을 사용한다.
 * @SQLRestriction을 사용하지 않아 soft-delete 레코드도 findByUserIdAndSpotId로 조회 가능하다.
 *
 * @author 황제연
 */
@Getter
@Entity
@Table(
    name = "saved_spot_archives",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_saved_spot_archives_user_spot",
            columnNames = {"user_id", "spot_id"}
        )
    },
    indexes = {
        @Index(name = "idx_saved_spot_archives_user_id", columnList = "user_id")
    }
)
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedSpotArchive extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "spot_id", nullable = false)
    private Long spotId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private SavedSpotArchive(Long userId, Long spotId) {
        this.userId = userId;
        this.spotId = spotId;
    }

    public boolean isActive() {
        return this.deletedAt == null;
    }

    public void restore() {
        this.deletedAt = null;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
