package com.ioes.photo.domain.alarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 내 스팟 알림 구독 변경 요청 DTO.
 *
 * @author 김성민
 */
@Schema(description = "내 스팟 알림 구독 변경 요청")
public record UpdateSpotAlarmRequest(
    @Schema(description = "알림 구독 활성화 여부", example = "true")
    @NotNull Boolean enabled
) {}
