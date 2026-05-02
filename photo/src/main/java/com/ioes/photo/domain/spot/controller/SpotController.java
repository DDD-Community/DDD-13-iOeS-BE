package com.ioes.photo.domain.spot.controller;

import com.ioes.photo.domain.spot.dto.SpotViewportResponse;
import com.ioes.photo.domain.spot.dto.ViewportRequest;
import com.ioes.photo.domain.spot.service.SpotQueryService;
import com.ioes.photo.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 스팟 조회 컨트롤러.
 *
 * @author 황제연
 */
@Tag(name = "스팟", description = "스팟 조회 API")
@RestController
@RequestMapping("/spots")
@RequiredArgsConstructor
public class SpotController {

    private final SpotQueryService spotQueryService;

    @Operation(summary = "뷰포트 내 스팟 목록 조회", description = "아이폰 뷰포트의 4개 꼭짓점 좌표 범위 내 스팟 목록을 반환합니다.")
    @PostMapping("/viewport")
    public ApiResponse<SpotViewportResponse> getSpotsInViewport(
        @RequestBody @Valid ViewportRequest request
    ) {
        return ApiResponse.success(spotQueryService.findSpotsInViewport(request));
    }
}
