package com.ioes.photo.domain.myspot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 나만의 스팟 수정 응답 DTO.
 *
 * @author 황제연
 */
@Schema(description = "나만의 스팟 수정 응답")
public record UpdateMySpotResponse(
    @Schema(description = "스팟 ID") Long spotId,
    @Schema(description = "스팟 상태 (수정으로는 바뀌지 않음)", example = "DRAFT") String status,
    @Schema(description = "대표 이미지 URL (없으면 null)") String imageUrl
) {}
