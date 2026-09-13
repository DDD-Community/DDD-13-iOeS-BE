package com.ioes.photo.domain.spot.controller;

import com.ioes.photo.domain.spot.dto.AdminSpotDetailResponse;
import com.ioes.photo.domain.spot.dto.AdminSpotListResponse;
import com.ioes.photo.domain.spot.dto.SpotReviewRequest;
import com.ioes.photo.domain.spot.dto.SpotReviewResultResponse;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.service.SpotReviewQueryService;
import com.ioes.photo.domain.spot.service.SpotReviewService;
import com.ioes.photo.global.auth.AdminOnly;
import com.ioes.photo.global.auth.CurrentUserId;
import com.ioes.photo.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 스팟 검수 어드민 컨트롤러.
 *
 * USER_ADMIN 권한을 가진 팀원만 접근 가능하며, 오픈 신청 건의 목록/상세 조회와 승인/반려를 처리한다.
 *
 * @author 황제연
 */
@Tag(name = "스팟 검수 어드민", description = "오픈 신청 스팟 검수(목록/상세/승인/반려) API")
@Validated
@RestController
@RequestMapping("/v1/admin/spots")
@RequiredArgsConstructor
@AdminOnly
public class SpotReviewAdminController {

    private final SpotReviewQueryService spotReviewQueryService;
    private final SpotReviewService spotReviewService;

    @Operation(
        summary = "검수 대상 스팟 목록 조회",
        description = "오픈 신청된 스팟을 정렬/검색/상태필터/페이징하여 조회합니다. "
            + "정렬은 서버 고정(재검토대기 → 검수중(오래된순) → 완료건(처리일시 최신순))이며, "
            + "status(상태 코드), q(스팟명·닉네임 부분일치)로 필터링합니다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping
    public ApiResponse<AdminSpotListResponse> getReviewSpots(
        @RequestParam(required = false) SpotStatus status,
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) int size
    ) {
        return ApiResponse.success(spotReviewQueryService.findReviewSpots(status, q, page, size));
    }

    @Operation(
        summary = "검수 대상 스팟 상세 조회",
        description = "유저가 등록한 내용 전체와 과거 반려 이력, 등록 유저 신뢰도 정보를 조회합니다. "
            + "미승인 건의 사진 URL은 만료되는 presigned URL로 내려갑니다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{spotId}")
    public ApiResponse<AdminSpotDetailResponse> getReviewSpotDetail(@PathVariable Long spotId) {
        return ApiResponse.success(spotReviewQueryService.getReviewSpotDetail(spotId));
    }

    @Operation(
        summary = "스팟 검수(승인/반려)",
        description = "오픈 신청 건을 승인 또는 반려합니다. 승인 시 즉시 지도에 공개되며, "
            + "반려 시 사유를 함께 저장합니다(기타 사유는 상세 설명 필수). "
            + "이미 처리된 건을 다시 처리하려 하면 409(SP004)로 응답합니다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/{spotId}/reviews")
    public ApiResponse<SpotReviewResultResponse> reviewSpot(
        @PathVariable Long spotId,
        @RequestBody @Valid SpotReviewRequest request,
        @CurrentUserId Long reviewerId
    ) {
        return ApiResponse.success(spotReviewService.review(spotId, request, reviewerId));
    }
}
