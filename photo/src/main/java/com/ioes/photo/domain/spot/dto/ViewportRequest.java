package com.ioes.photo.domain.spot.dto;

import com.ioes.photo.global.common.validation.Latitude;
import com.ioes.photo.global.common.validation.Longitude;
import jakarta.validation.constraints.NotNull;

/**
 * 지도 뷰포트 4개 꼭짓점 좌표 요청.
 *
 * @author 황제연
 */
public record ViewportRequest(
    @NotNull @Latitude Double topLeftLat,
    @NotNull @Longitude Double topLeftLng,
    @NotNull @Latitude Double topRightLat,
    @NotNull @Longitude Double topRightLng,
    @NotNull @Latitude Double bottomLeftLat,
    @NotNull @Longitude Double bottomLeftLng,
    @NotNull @Latitude Double bottomRightLat,
    @NotNull @Longitude Double bottomRightLng
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
