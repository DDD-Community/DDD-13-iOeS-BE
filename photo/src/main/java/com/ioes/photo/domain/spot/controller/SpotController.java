package com.ioes.photo.domain.spot.controller;

import com.ioes.photo.domain.spot.dto.SpotDetailResponse;
import com.ioes.photo.domain.spot.dto.SpotListResponse;
import com.ioes.photo.domain.spot.dto.SpotViewportResponse;
import com.ioes.photo.domain.spot.dto.ViewportRequest;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.service.SpotQueryService;
import com.ioes.photo.global.common.response.ApiResponse;
import com.ioes.photo.global.common.validation.Latitude;
import com.ioes.photo.global.common.validation.Longitude;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@RequestMapping("/v1/spots")
@RequiredArgsConstructor
public class SpotController {

    private final SpotQueryService spotQueryService;

    @Operation(summary = "스팟 상세 조회", description = "스팟 ID로 상세 정보(이미지, 한 줄 코멘트, 기록일자/시간, 날씨, 혼잡도, 일몰시간, 북마크 여부 등)를 반환합니다. 비로그인 시 isBookmarked/isMySpot은 항상 false입니다.")
    @GetMapping("/{spotId}")
    public ApiResponse<SpotDetailResponse> getSpotDetail(
        @PathVariable Long spotId,
        Authentication authentication
    ) {
        return ApiResponse.success(spotQueryService.findSpotDetail(spotId, extractUserId(authentication)));
    }

    private static Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Operation(summary = "뷰포트 내 스팟 목록 조회", description = "지도 뷰포트의 4개 꼭짓점 좌표 범위 내 스팟 목록을 반환합니다.")
    @GetMapping("/viewport")
    public ApiResponse<SpotViewportResponse> getSpotsInViewport(
        @RequestParam @NotNull @Latitude Double topLeftLat,
        @RequestParam @NotNull @Longitude Double topLeftLng,
        @RequestParam @NotNull @Latitude Double topRightLat,
        @RequestParam @NotNull @Longitude Double topRightLng,
        @RequestParam @NotNull @Latitude Double bottomLeftLat,
        @RequestParam @NotNull @Longitude Double bottomLeftLng,
        @RequestParam @NotNull @Latitude Double bottomRightLat,
        @RequestParam @NotNull @Longitude Double bottomRightLng
    ) {
        return ApiResponse.success(spotQueryService.findSpotsInViewport(
            new ViewportRequest(topLeftLat, topLeftLng, topRightLat, topRightLng,
                                bottomLeftLat, bottomLeftLng, bottomRightLat, bottomRightLng)
        ));
    }

    @Operation(
        summary = "스팟 리스트 조회",
        description = "스팟 목록을 6개 단위로 페이징 조회합니다. 위도/경도를 함께 전달하면 가까운 순으로 정렬되며, 생략 시 최신순으로 반환합니다."
    )
    @GetMapping
    public ApiResponse<SpotListResponse> getSpots(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(required = false) SpotTheme theme,
        @RequestParam(required = false) @Latitude Double latitude,
        @RequestParam(required = false) @Longitude Double longitude
    ) {
        return ApiResponse.success(spotQueryService.findSpots(page, theme, latitude, longitude));
    }
}