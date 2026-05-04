package com.ioes.photo.domain.spot.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 지도 뷰포트 4개 꼭짓점 좌표 요청.
 */
public record ViewportRequest(
    @NotNull Double topLeftLat,
    @NotNull Double topLeftLng,
    @NotNull Double topRightLat,
    @NotNull Double topRightLng,
    @NotNull Double bottomLeftLat,
    @NotNull Double bottomLeftLng,
    @NotNull Double bottomRightLat,
    @NotNull Double bottomRightLng
) {

    public double minLat() {
        return Math.min(Math.min(topLeftLat, topRightLat),
                        Math.min(bottomLeftLat, bottomRightLat));
    }

    public double maxLat() {
        return Math.max(Math.max(topLeftLat, topRightLat),
                        Math.max(bottomLeftLat, bottomRightLat));
    }

    public double minLng() {
        return Math.min(Math.min(topLeftLng, topRightLng),
                        Math.min(bottomLeftLng, bottomRightLng));
    }

    public double maxLng() {
        return Math.max(Math.max(topLeftLng, topRightLng),
                        Math.max(bottomLeftLng, bottomRightLng));
    }
}
