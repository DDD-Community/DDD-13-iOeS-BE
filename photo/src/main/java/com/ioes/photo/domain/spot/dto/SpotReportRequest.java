package com.ioes.photo.domain.spot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 스팟 신고 요청 DTO.
 *
 * @author 황제연
 */
@Schema(description = "스팟 신고 요청")
public record SpotReportRequest(
    @Schema(description = "신고 내용 (최소 5자, 최대 200자)")
    @NotBlank(message = "신고 내용은 필수입니다.")
    @Size(min = 5, max = 200, message = "신고 내용은 5자 이상 200자 이하로 입력해주세요.")
    String content
) {}
