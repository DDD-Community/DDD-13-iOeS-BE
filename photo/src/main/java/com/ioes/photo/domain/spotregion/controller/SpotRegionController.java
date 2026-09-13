package com.ioes.photo.domain.spotregion.controller;

import com.ioes.photo.domain.spotregion.dto.RegionListResponse;
import com.ioes.photo.domain.spotregion.service.SpotRegionQueryService;
import com.ioes.photo.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지역 조회 컨트롤러.
 *
 * @author 황제연
 */
@Tag(name = "지역", description = "지역 필터 조회 API")
@RestController
@RequestMapping("/v1/regions")
@RequiredArgsConstructor
public class SpotRegionController {

    private final SpotRegionQueryService spotRegionQueryService;

    @Operation(summary = "활성화된 지역 목록 조회", description = "지도뷰/리스트뷰 지역 필터에 사용할 활성화된 지역(region_id, region_name) 목록을 region_id 오름차순으로 반환합니다.")
    @SecurityRequirements
    @GetMapping
    public ApiResponse<RegionListResponse> getActiveRegions() {
        return ApiResponse.success(spotRegionQueryService.findActiveRegions());
    }
}
