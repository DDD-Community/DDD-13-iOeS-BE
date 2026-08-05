package com.ioes.photo.domain.myspot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 나만의 스팟 등록 응답 DTO.
 *
 * @author 김성민
 */
@Schema(description = "나만의 스팟 등록 응답")
public record CreateMySpotResponse(
    @Schema(description = "등록된 스팟 ID") Long spotId,
    @Schema(description = "스팟 상태 (등록 직후엔 항상 DRAFT)") String status,
    @Schema(description = "원본 이미지 URL") String imageUrl
) {}
