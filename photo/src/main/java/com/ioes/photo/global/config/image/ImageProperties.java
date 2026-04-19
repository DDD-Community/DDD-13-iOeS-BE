package com.ioes.photo.global.config.image;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 이미지 처리 관련 설정 프로퍼티.
 *
 * thumbnail.width - 썸네일 가로 최대 픽셀 (기본: 400)
 * thumbnail.height - 썸네일 세로 최대 픽셀 (기본: 400)
 * presigned-url-expiry-minutes - Presigned URL 유효 시간(분) (기본: 60)
 *
 * @author 황제연
 */
@ConfigurationProperties(prefix = "app.image")
public record ImageProperties(
    ThumbnailProperties thumbnail,
    int presignedUrlExpiryMinutes
) {
    public record ThumbnailProperties(int width, int height) {}
}