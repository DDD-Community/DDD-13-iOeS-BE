package com.ioes.photo.domain.myspot.controller;

import com.ioes.photo.domain.myspot.dto.MySpotListResponse;
import com.ioes.photo.domain.myspot.service.MySpotService;
import com.ioes.photo.global.common.response.ApiResponse;
import com.ioes.photo.global.common.validation.Latitude;
import com.ioes.photo.global.common.validation.Longitude;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 나만의 스팟 조회 컨트롤러.
 *
 * @author 김성민
 */
@Tag(name = "나만의 스팟", description = "사용자가 등록한 스팟 조회 API")
@Validated
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class MySpotController {

    private final MySpotService mySpotService;

    @Operation(
        summary = "나만의 스팟 목록 조회",
        description = "사용자가 등록한 스팟 목록을 6개 단위로 페이징 조회합니다. "
            + "검수 대기(PENDING) 및 공개(PUBLISHED) 상태를 노출하며 반려(REJECTED)는 제외됩니다. "
            + "위도/경도를 함께 전달하면 거리(km)도 함께 반환됩니다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/users/me/my-spots")
    public ApiResponse<MySpotListResponse> getMySpots(
        Authentication authentication,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(required = false) @Latitude Double latitude,
        @RequestParam(required = false) @Longitude Double longitude
    ) {
        Long userId = Long.parseLong(authentication.getName());
        return ApiResponse.success(mySpotService.findMySpots(userId, page, latitude, longitude));
    }
}
