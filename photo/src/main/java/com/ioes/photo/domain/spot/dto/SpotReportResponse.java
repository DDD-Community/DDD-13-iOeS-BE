package com.ioes.photo.domain.spot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 스팟 신고 응답 DTO.
 *
 * @author 황제연
 */
@Schema(description = "스팟 신고 응답")
public record SpotReportResponse(
    @Schema(description = "생성된 신고 ID") Long reportId
) {}
