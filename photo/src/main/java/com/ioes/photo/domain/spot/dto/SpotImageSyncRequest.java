package com.ioes.photo.domain.spot.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 스팟 이미지 동기화 요청 DTO.
 *
 * S3에 직접 업로드된 이미지를 DB에 등록하고 썸네일을 생성할 때 사용한다.
 */
public record SpotImageSyncRequest(
    @NotBlank String imageKey,
    String originalFilename,
    String contentType
) {}