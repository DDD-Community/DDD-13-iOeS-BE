package com.ioes.photo.domain.spotlike.entity;

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
 * 스팟 좋아요(추천).
 *
 * 취소를 논리삭제로 처리해 (user_id, spot_id) UNIQUE 제약을 중복 방어에 계속 활용한다.
 * 물리삭제였다면 재좋아요마다 INSERT 경합을 다시 다뤄야 한다.
 * spots 와는 연관관계 매핑 없이 spotId(PK) 로 연결한다.
 *
 * @author 황제연
 */
@Getter
@Entity
@Table(
    name = "spot_likes",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_spot_likes_user_spot", columnNames = {"user_id", "spot_id"}),
    },
    indexes = {
        @Index(name = "idx_spot_likes_user_id", columnList = "user_id"),
        @Index(name = "idx_spot_likes_spot_id", columnList = "spot_id"),
    }
)
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotLike extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "spot_id", nullable = false)
    private Long spotId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private SpotLike(Long userId, Long spotId) {
        this.userId = userId;
        this.spotId = spotId;
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    public void restore() {
        this.deletedAt = null;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
