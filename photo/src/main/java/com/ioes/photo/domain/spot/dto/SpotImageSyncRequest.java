package com.ioes.photo.domain.spot.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 스팟 이미지 동기화 요청 DTO.
 *
 * S3에 직접 업로드된 이미지를 DB에 등록하고 썸네일을 생성할 때 사용한다.
 *
 * @author 황제연
 */
@Schema(description = "스팟 이미지 동기화 요청")
public record SpotImageSyncRequest(
    @Schema(description = "S3 이미지 키") @NotBlank String imageKey,
    @Schema(description = "원본 파일명") String originalFilename,
    @Schema(description = "Content-Type") String contentType,
    @Schema(description = "촬영 날짜 (yyyy-MM-dd)") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate recordedDate,
    @Schema(description = "촬영 시각 (HH:mm)") @JsonFormat(pattern = "HH:mm") LocalTime recordedTime
) {}
