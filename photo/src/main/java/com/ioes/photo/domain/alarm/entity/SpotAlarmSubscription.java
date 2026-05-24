package com.ioes.photo.domain.alarm.entity;

import com.ioes.photo.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 내 스팟 촬영조건 알림 구독 엔티티.
 *
 * 사용자가 등록한 스팟의 촬영조건(테마/날씨/일몰 등) 알림 수신 여부를 보관한다.
 * user_id + spot_id 조합에 UNIQUE 제약을 두고 enabled 플래그로 on/off를 관리한다.
 *
 * @author 김성민
 */
@Getter
@Entity
@Table(
    name = "spot_alarm_subscriptions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_spot_alarm_subscriptions_user_spot",
            columnNames = {"user_id", "spot_id"}
        )
    },
    indexes = {
        @Index(name = "idx_spot_alarm_subscriptions_user_id", columnList = "user_id")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotAlarmSubscription extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "spot_id", nullable = false)
    private Long spotId;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Builder
    private SpotAlarmSubscription(Long userId, Long spotId, boolean enabled) {
        this.userId = userId;
        this.spotId = spotId;
        this.enabled = enabled;
    }

    public static SpotAlarmSubscription create(Long userId, Long spotId) {
        return new SpotAlarmSubscription(userId, spotId, true);
    }

    public void updateEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
