package com.ioes.photo.domain.spotlike.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 스팟 좋아요 처리 응답 DTO.
 *
 * likeCount 는 처리 직후 서버에 반영된 값이다. 여러 사용자가 동시에 누르는 상황에서
 * 클라이언트는 이 값을 기준으로 화면을 다시 맞춘다.
 *
 * @author 황제연
 */
@Schema(description = "스팟 좋아요 응답")
public record SpotLikeResponse(
    @Schema(description = "서버에 최종 반영된 좋아요 수", example = "12") long likeCount,
    @Schema(description = "요청한 사용자의 좋아요 여부") boolean isLiked
) {}
