package com.ioes.photo.global.config.image;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 이미지 처리 관련 설정 프로퍼티.
 *
 * thumbnail.width - 썸네일 가로 최대 픽셀 (기본: 400)
 * thumbnail.height - 썸네일 세로 최대 픽셀 (기본: 400)
 * presigned-url-expiry-minutes - Presigned URL 유효 시간(분) (기본: 60)
 * resize.pool-size - 리사이즈 전용 스레드풀 크기 (기본: 4)
 * resize.timeout-seconds - 리사이즈 1건당 최대 대기 시간(초) (기본: 30)
 * resize.memory-budget-ratio - JVM 최대 힙(Runtime.maxMemory()) 대비 리사이즈 동시 처리에 허용할 비율 (기본: 0.5)
 * resize.vips-fallback-threshold-ratio - 단일 이미지 추정 메모리가 힙의 이 비율을 넘으면 libvips로 위임 (기본: 0.4)
 *
 * @author 황제연
 */
@ConfigurationProperties(prefix = "app.image")
public record ImageProperties(
    ThumbnailProperties thumbnail,
    int presignedUrlExpiryMinutes,
    @DefaultValue ResizeProperties resize
) {
    public record ThumbnailProperties(int width, int height) {}

    public record ResizeProperties(
        @DefaultValue("4") int poolSize,
        @DefaultValue("30") int timeoutSeconds,
        @DefaultValue("0.5") double memoryBudgetRatio,
        @DefaultValue("0.4") double vipsFallbackThresholdRatio
    ) {}
}