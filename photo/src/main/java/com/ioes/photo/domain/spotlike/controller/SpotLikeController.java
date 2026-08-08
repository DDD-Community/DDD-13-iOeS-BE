package com.ioes.photo.domain.spotlike.controller;

import com.ioes.photo.domain.spotlike.dto.SpotLikeResponse;
import com.ioes.photo.domain.spotlike.service.SpotLikeService;
import com.ioes.photo.global.auth.CurrentUserId;
import com.ioes.photo.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 스팟 좋아요(추천) 컨트롤러.
 *
 * @author 황제연
 */
@Tag(name = "스팟 좋아요", description = "스팟 추천(좋아요) 등록/취소 API")
@Validated
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class SpotLikeController {

    private final SpotLikeService spotLikeService;

    @Operation(
        summary = "스팟 좋아요",
        description = "스팟에 좋아요(추천)를 등록합니다. 공개(PUBLISHED)된 스팟에만 허용되며, "
            + "그 외 상태의 유저 스팟은 SL003으로 응답합니다. 관리자 큐레이션 스팟은 상태와 무관하게 허용됩니다. "
            + "비로그인 상태로 호출하면 401(C004)로 응답합니다. "
            + "응답의 likeCount는 서버에 최종 반영된 값이므로 화면은 이 값을 기준으로 맞추면 됩니다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/spots/{spotId}/likes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SpotLikeResponse> addLike(
        @CurrentUserId Long userId,
        @PathVariable Long spotId
    ) {
        return ApiResponse.success(spotLikeService.addLike(userId, spotId));
    }

    @Operation(
        summary = "스팟 좋아요 취소",
        description = "등록한 좋아요를 취소합니다. 좋아요하지 않은 스팟이면 SL002로 응답합니다. "
            + "비로그인 상태로 호출하면 401(C004)로 응답합니다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/spots/{spotId}/likes")
    public ApiResponse<SpotLikeResponse> removeLike(
        @CurrentUserId Long userId,
        @PathVariable Long spotId
    ) {
        return ApiResponse.success(spotLikeService.removeLike(userId, spotId));
    }
}
