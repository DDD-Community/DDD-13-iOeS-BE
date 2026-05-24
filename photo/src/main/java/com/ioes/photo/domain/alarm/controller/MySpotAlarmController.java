package com.ioes.photo.domain.alarm.controller;

import com.ioes.photo.domain.alarm.dto.SpotAlarmResponse;
import com.ioes.photo.domain.alarm.dto.UpdateSpotAlarmRequest;
import com.ioes.photo.domain.alarm.service.SpotAlarmService;
import com.ioes.photo.global.auth.CurrentUserId;
import com.ioes.photo.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내 스팟 촬영조건 알림 구독 컨트롤러.
 *
 * @author 김성민
 */
@Tag(name = "내 스팟 알림", description = "내 스팟 촬영조건 알림 구독 조회/변경 API")
@RestController
@RequestMapping("/v1/users/me/my-spots")
@RequiredArgsConstructor
public class MySpotAlarmController {

    private final SpotAlarmService spotAlarmService;

    @Operation(
        summary = "내 스팟 알림 구독 조회",
        description = "본인이 등록한 스팟의 촬영조건 알림 구독 상태를 조회합니다. 구독 이력이 없으면 enabled=false로 반환합니다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{spotId}/alarm")
    public ApiResponse<SpotAlarmResponse> getAlarm(
        @CurrentUserId Long userId,
        @PathVariable Long spotId
    ) {
        return ApiResponse.success(spotAlarmService.getSubscription(userId, spotId));
    }

    @Operation(
        summary = "내 스팟 알림 구독 변경",
        description = "본인이 등록한 스팟의 촬영조건 알림 구독을 켜거나 끕니다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/{spotId}/alarm")
    public ApiResponse<SpotAlarmResponse> updateAlarm(
        @CurrentUserId Long userId,
        @PathVariable Long spotId,
        @RequestBody @Valid UpdateSpotAlarmRequest request
    ) {
        return ApiResponse.success(spotAlarmService.updateEnabled(userId, spotId, request.enabled()));
    }
}
