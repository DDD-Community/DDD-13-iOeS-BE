package com.ioes.photo.domain.spot.controller;

import com.ioes.photo.domain.spot.dto.SpotListResponse;
import com.ioes.photo.domain.spot.dto.SpotViewportResponse;
import com.ioes.photo.domain.spot.dto.ViewportRequest;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.service.SpotQueryService;
import com.ioes.photo.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 스팟 조회 컨트롤러.
 *
 * @author 황제연
 */
@Tag(name = "스팟", description = "스팟 조회 API")
@Validated
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

    @Operation(
        summary = "스팟 리스트 조회",
        description = "스팟 목록을 6개 단위로 페이징 조회합니다. 위도/경도를 함께 전달하면 가까운 순으로 정렬되며, 생략 시 최신순으로 반환합니다."
    )
    @GetMapping
    public ApiResponse<SpotListResponse> getSpots(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(required = false) SpotTheme theme,
        @RequestParam(required = false) Double latitude,
        @RequestParam(required = false) Double longitude
    ) {
        return ApiResponse.success(spotQueryService.findSpots(page, theme, latitude, longitude));
    }
}