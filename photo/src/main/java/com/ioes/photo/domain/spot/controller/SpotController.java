package com.ioes.photo.domain.spot.controller;

import com.ioes.photo.domain.spot.dto.SpotDetailResponse;
import com.ioes.photo.domain.spot.dto.SpotListResponse;
import com.ioes.photo.domain.spot.dto.SpotPreviewResponse;
import com.ioes.photo.domain.spot.dto.SpotViewportResponse;
import com.ioes.photo.domain.spot.dto.ViewportRequest;
import com.ioes.photo.domain.spot.enums.SortType;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.service.SpotQueryService;
import com.ioes.photo.global.auth.CurrentUserId;
import com.ioes.photo.global.common.response.ApiResponse;
import com.ioes.photo.global.common.validation.Latitude;
import com.ioes.photo.global.common.validation.Longitude;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
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

    @Operation(
        summary = "스팟 상세 조회",
        description = "스팟 ID로 상세 정보(이미지, 한 줄 코멘트, 기록일자/시간, 날씨, 혼잡도, 일몰시간, 북마크/좋아요 여부 등)를 반환합니다. "
            + "공개되지 않은 스팟은 등록한 본인에게만 보이며, 그 외에는 404로 응답합니다. "
            + "반려된 내 스팟이면 rejection에 반려 사유가 함께 내려갑니다(타인에게는 노출되지 않습니다). "
            + "조회수는 공개 상태의 스팟을 등록자 외의 사용자가 볼 때만 증가합니다. "
            + "비로그인 시 isBookmarked/isLiked/isMySpot은 항상 false입니다."
    )
    @SecurityRequirements
    @GetMapping("/{spotId}")
    public ApiResponse<SpotDetailResponse> getSpotDetail(
        @PathVariable Long spotId,
        @CurrentUserId Long userId
    ) {
        return ApiResponse.success(spotQueryService.findSpotDetail(spotId, userId));
    }

    @Operation(summary = "뷰포트 내 스팟 목록 조회", description = "지도 뷰포트의 4개 꼭짓점 좌표 범위 내 스팟 목록을 반환합니다. 비로그인 시 isMySpot은 항상 false입니다.")
    @SecurityRequirements
    @GetMapping("/viewport")
    public ApiResponse<SpotViewportResponse> getSpotsInViewport(
        @Parameter(description = "좌상단 위도", example = "37.58") @RequestParam @NotNull @Latitude Double topLeftLat,
        @Parameter(description = "좌상단 경도", example = "126.97") @RequestParam @NotNull @Longitude Double topLeftLng,
        @Parameter(description = "우상단 위도", example = "37.58") @RequestParam @NotNull @Latitude Double topRightLat,
        @Parameter(description = "우상단 경도", example = "127.05") @RequestParam @NotNull @Longitude Double topRightLng,
        @Parameter(description = "좌하단 위도", example = "37.52") @RequestParam @NotNull @Latitude Double bottomLeftLat,
        @Parameter(description = "좌하단 경도", example = "126.97") @RequestParam @NotNull @Longitude Double bottomLeftLng,
        @Parameter(description = "우하단 위도", example = "37.52") @RequestParam @NotNull @Latitude Double bottomRightLat,
        @Parameter(description = "우하단 경도", example = "127.05") @RequestParam @NotNull @Longitude Double bottomRightLng,
        @Parameter(description = "테마 필터 (SUNSET=노을, YUNSEUL=윤슬, SUNLIGHT=햇살, NIGHT_VIEW=야경), 미전달 시 전체")
        @RequestParam(required = false) SpotTheme theme,
        @CurrentUserId Long userId
    ) {
        return ApiResponse.success(spotQueryService.findSpotsInViewport(
            new ViewportRequest(topLeftLat, topLeftLng, topRightLat, topRightLng,
                                bottomLeftLat, bottomLeftLng, bottomRightLat, bottomRightLng),
            theme,
            userId
        ));
    }

    @Operation(
        summary = "스팟 리스트 조회",
        description = "공개(PUBLISHED)된 스팟 목록을 6개 단위로 페이징 조회합니다. "
            + "sort=DISTANCE 시 위도/경도 필수이며 가까운 순으로 정렬됩니다. "
            + "sort=RECOMMENDED(기본값) 시 좋아요 많은 순으로 정렬되며, 동률은 북마크 수로 가릅니다. "
            + "비로그인 시 isBookmarked/isLiked는 항상 false입니다."
    )
    @SecurityRequirements
    @GetMapping
    public ApiResponse<SpotListResponse> getSpots(
        @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") @Min(0) int page,
        @Parameter(description = "테마 필터 (SUNSET=노을, YUNSEUL=윤슬, SUNLIGHT=햇살, NIGHT_VIEW=야경), 미전달 시 전체")
        @RequestParam(required = false) SpotTheme theme,
        @Parameter(description = "사용자 위도 (sort=DISTANCE 시 필수)", example = "37.55") @RequestParam(required = false) @Latitude Double latitude,
        @Parameter(description = "사용자 경도 (sort=DISTANCE 시 필수)", example = "126.99") @RequestParam(required = false) @Longitude Double longitude,
        @Parameter(description = "정렬 기준 (RECOMMENDED=좋아요순 기본값, DISTANCE=거리순)") @RequestParam(required = false, defaultValue = "RECOMMENDED") SortType sort,
        @CurrentUserId Long userId
    ) {
        return ApiResponse.success(spotQueryService.findSpots(page, theme, latitude, longitude, sort, userId));
    }

    @Operation(
        summary = "스팟 미리보기 조회",
        description = "스팟 ID와 사용자 위치를 기반으로 간략 정보(스팟명, 내 스팟 여부, 테마, 북마크/좋아요 수, 거리, 주소)를 반환합니다. "
            + "공개되지 않은 스팟은 등록한 본인에게만 보이며, 그 외에는 404로 응답합니다. "
            + "위도/경도 미전달 시 거리 정보는 null입니다. 비로그인 시 isMySpot은 항상 false입니다."
    )
    @SecurityRequirements
    @GetMapping("/{spotId}/preview")
    public ApiResponse<SpotPreviewResponse> getSpotPreview(
        @PathVariable Long spotId,
        @Parameter(description = "사용자 위도 (거리 계산용)", example = "37.55") @RequestParam(required = false) @Latitude Double latitude,
        @Parameter(description = "사용자 경도 (거리 계산용)", example = "126.99") @RequestParam(required = false) @Longitude Double longitude,
        @CurrentUserId Long userId
    ) {
        return ApiResponse.success(spotQueryService.findSpotPreview(spotId, latitude, longitude, userId));
    }
}
