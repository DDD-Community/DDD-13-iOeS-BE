package com.ioes.photo.domain.spot.dto;

import com.ioes.photo.domain.spot.enums.SpotReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 스팟 신고 요청 DTO.
 *
 * @author 황제연
 */
public record SpotReportRequest(
    @NotNull(message = "신고 유형은 필수입니다.")
    SpotReportType type,

    @NotBlank(message = "신고 내용은 필수입니다.")
    @Size(max = 5000, message = "신고 내용은 5000자를 초과할 수 없습니다.")
    String content
) {}
