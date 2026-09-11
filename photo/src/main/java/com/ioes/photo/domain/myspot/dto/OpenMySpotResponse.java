package com.ioes.photo.domain.myspot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 나만의 스팟 오픈 신청(검수 요청) 응답 DTO.
 *
 * @author 황제연
 */
@Schema(description = "나만의 스팟 오픈 신청 응답")
public record OpenMySpotResponse(
    @Schema(description = "오픈 신청한 스팟 ID") Long spotId,
    @Schema(description = "전이된 스팟 상태 (PENDING=검수중, RE_REVIEW_PENDING=재검토대기)") String status
) {}
