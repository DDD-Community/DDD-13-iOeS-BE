package com.ioes.photo.domain.notification.controller;

import com.ioes.photo.domain.notification.dto.SpotOpenReviewHistoryCheckResponse;
import com.ioes.photo.domain.notification.dto.SpotOpenReviewHistoryListResponse;
import com.ioes.photo.domain.notification.service.SpotOpenReviewHistoryService;
import com.ioes.photo.global.auth.CurrentUserId;
import com.ioes.photo.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 스팟 검수완료 알림 히스토리 컨트롤러.
 *
 * @author 황제연
 */
@Tag(name = "검수완료 알림 히스토리", description = "스팟 검수(승인/반려) 완료 알림 히스토리 조회/확인 API")
@RestController
@RequestMapping("/v1/users/me/spot-open-review-histories")
@RequiredArgsConstructor
public class SpotOpenReviewHistoryController {

    private final SpotOpenReviewHistoryService spotOpenReviewHistoryService;

    @Operation(
        summary = "검수완료 히스토리 확인여부 업데이트",
        description = "본인 소유의 검수완료 히스토리를 확인(check_yn=Y) 처리합니다. "
            + "이미 확인된 히스토리를 다시 요청해도 에러 없이 성공 응답합니다(멱등). "
            + "히스토리가 없으면 404, 본인 소유가 아니면 403으로 응답합니다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PatchMapping("/{historyId}/check-status")
    public ApiResponse<SpotOpenReviewHistoryCheckResponse> updateCheckStatus(
        @CurrentUserId Long userId,
        @PathVariable Long historyId
    ) {
        return ApiResponse.success(spotOpenReviewHistoryService.markChecked(userId, historyId));
    }

    @Operation(
        summary = "미확인 검수완료 히스토리 목록 조회",
        description = "본인 소유의 미확인(check_yn=N) 검수완료 히스토리를 승인/반려 목록으로 분리해 조회합니다. "
            + "페이징은 적용하지 않습니다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping
    public ApiResponse<SpotOpenReviewHistoryListResponse> getUncheckedHistories(
        @CurrentUserId Long userId
    ) {
        return ApiResponse.success(spotOpenReviewHistoryService.findUnchecked(userId));
    }
}
