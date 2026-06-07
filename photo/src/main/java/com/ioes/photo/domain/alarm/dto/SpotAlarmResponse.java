package com.ioes.photo.domain.alarm.dto;

import com.ioes.photo.domain.alarm.entity.SpotAlarmSubscription;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 내 스팟 알림 구독 응답 DTO.
 *
 * @author 김성민
 */
@Schema(description = "내 스팟 알림 구독 응답")
public record SpotAlarmResponse(
    @Schema(description = "스팟 ID") Long spotId,
    @Schema(description = "알림 구독 활성화 여부") boolean enabled
) {

    public static SpotAlarmResponse of(SpotAlarmSubscription subscription) {
        return new SpotAlarmResponse(subscription.getSpotId(), subscription.isEnabled());
    }

    public static SpotAlarmResponse disabled(Long spotId) {
        return new SpotAlarmResponse(spotId, false);
    }
}
