package com.ioes.photo.domain.spot.controller;

import com.ioes.photo.domain.spot.dto.SpotReportRequest;
import com.ioes.photo.domain.spot.dto.SpotReportResponse;
import com.ioes.photo.domain.spot.service.SpotReportService;
import com.ioes.photo.global.auth.CurrentUserId;
import com.ioes.photo.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 스팟 신고 컨트롤러.
 *
 * @author 황제연
 */
@Tag(name = "스팟 신고", description = "스팟 잘못된 정보 신고 API")
@RestController
@RequestMapping("/v1/spots")
@RequiredArgsConstructor
public class SpotReportController {

    private final SpotReportService spotReportService;

    @Operation(
        summary = "스팟 신고",
        description = "잘못된 정보가 있는 스팟을 신고합니다. 신고 내용은 5자 이상 입력해야 합니다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/{spotId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SpotReportResponse> report(
        @CurrentUserId Long userId,
        @PathVariable Long spotId,
        @RequestBody @Valid SpotReportRequest request
    ) {
        return ApiResponse.success(spotReportService.report(userId, spotId, request));
    }
}
