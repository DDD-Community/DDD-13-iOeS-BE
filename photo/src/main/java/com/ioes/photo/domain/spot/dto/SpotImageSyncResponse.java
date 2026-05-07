package com.ioes.photo.domain.spot.dto;

/**
 * 스팟 이미지 동기화 응답 DTO.
 */
public record SpotImageSyncResponse(
    String imageUrl,
    String thumbnailUrl
) {}