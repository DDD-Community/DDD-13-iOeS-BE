package com.ioes.photo.domain.spot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 스팟 이미지 동기화 응답 DTO.
 *
 * @author 황제연
 */
@Schema(description = "스팟 이미지 동기화 응답")
public record SpotImageSyncResponse(
    @Schema(description = "원본 이미지 URL") String imageUrl,
    @Schema(description = "썸네일 이미지 URL") String thumbnailUrl
) {}