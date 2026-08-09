package com.ioes.photo.domain.spot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 스팟 검수(승인/반려) 처리 결과 응답 DTO.
 *
 * @author 황제연
 */
@Schema(description = "스팟 검수 처리 결과 응답")
public record SpotReviewResultResponse(
    @Schema(description = "처리된 스팟 ID") Long spotId,
    @Schema(description = "전이된 스팟 상태 (PUBLISHED=승인, REJECTED=반려)") String status
) {}
