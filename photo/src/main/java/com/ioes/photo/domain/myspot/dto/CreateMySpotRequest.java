package com.ioes.photo.domain.myspot.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.global.common.validation.Latitude;
import com.ioes.photo.global.common.validation.Longitude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 나만의 스팟 등록 요청 DTO.
 *
 * @author 김성민
 */
@Schema(description = "나만의 스팟 등록 요청")
public record CreateMySpotRequest(
    @Schema(description = "스팟 이름", example = "한강 노을 명소")
    @NotBlank @Size(max = 100) String name,

    @Schema(description = "스팟 테마", example = "SUNSET")
    @NotNull SpotTheme theme,

    @Schema(description = "위도", example = "37.5326")
    @NotNull @Latitude Double latitude,

    @Schema(description = "경도", example = "126.9905")
    @NotNull @Longitude Double longitude,

    @Schema(description = "한 줄 코멘트", nullable = true)
    String comment,

    @Schema(description = "촬영 일자 (yyyy-MM-dd)", nullable = true)
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate recordedDate,

    @Schema(description = "촬영 시각 (HH:mm)", nullable = true)
    @JsonFormat(pattern = "HH:mm") LocalTime recordedTime
) {}
