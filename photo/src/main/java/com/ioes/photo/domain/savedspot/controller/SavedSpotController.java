package com.ioes.photo.domain.savedspot.controller;

import com.ioes.photo.domain.savedspot.dto.BookmarkResponse;
import com.ioes.photo.domain.savedspot.dto.SavedSpotListResponse;
import com.ioes.photo.domain.savedspot.service.SavedSpotService;
import com.ioes.photo.global.common.response.ApiResponse;
import com.ioes.photo.global.common.validation.Latitude;
import com.ioes.photo.global.common.validation.Longitude;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 스팟 북마크 및 저장 스팟 목록 컨트롤러.
 *
 * @author 황제연
 */
@Tag(name = "북마크 및 저장된 스팟 관리", description = "스팟 북마크 관리 및 저장된 스팟 관리 API")
@Validated
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class SavedSpotController {

    private final SavedSpotService savedSpotService;

    @Operation(summary = "북마크 지정", description = "스팟을 북마크하여 저장 목록에 추가합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/spots/{spotId}/bookmarks")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BookmarkResponse> addBookmark(
        Authentication authentication,
        @PathVariable Long spotId
    ) {
        Long userId = Long.parseLong(authentication.getName());
        return ApiResponse.success(savedSpotService.addBookmark(userId, spotId));
    }

    @Operation(summary = "북마크 해제", description = "저장 목록에서 스팟 북마크를 해제합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/spots/{spotId}/bookmarks")
    public ApiResponse<BookmarkResponse> removeBookmark(
        Authentication authentication,
        @PathVariable Long spotId
    ) {
        Long userId = Long.parseLong(authentication.getName());
        return ApiResponse.success(savedSpotService.removeBookmark(userId, spotId));
    }

    @Operation(
        summary = "저장된 스팟 목록 조회",
        description = "북마크한 스팟 목록을 6개 단위로 페이징 조회합니다. 위도/경도를 함께 전달하면 거리(km)도 함께 반환됩니다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/users/me/saved-spots")
    public ApiResponse<SavedSpotListResponse> getSavedSpots(
        Authentication authentication,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(required = false) @Latitude Double latitude,
        @RequestParam(required = false) @Longitude Double longitude
    ) {
        Long userId = Long.parseLong(authentication.getName());
        return ApiResponse.success(savedSpotService.findSavedSpots(userId, page, latitude, longitude));
    }
}
