package com.ioes.photo.domain.spot.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * 아이폰 뷰포트 4개 꼭짓점 좌표 요청.
 *
 * @author 황제연
 */
public record ViewportRequest(
    @NotNull @Valid Coordinate topLeft,
    @NotNull @Valid Coordinate topRight,
    @NotNull @Valid Coordinate bottomLeft,
    @NotNull @Valid Coordinate bottomRight
) {

    public record Coordinate(
        @NotNull Double lat,
        @NotNull Double lng
    ) {}

    public double minLat() {
        return Math.min(Math.min(topLeft.lat(), topRight.lat()),
                        Math.min(bottomLeft.lat(), bottomRight.lat()));
    }

    public double maxLat() {
        return Math.max(Math.max(topLeft.lat(), topRight.lat()),
                        Math.max(bottomLeft.lat(), bottomRight.lat()));
    }

    public double minLng() {
        return Math.min(Math.min(topLeft.lng(), topRight.lng()),
                        Math.min(bottomLeft.lng(), bottomRight.lng()));
    }

    public double maxLng() {
        return Math.max(Math.max(topLeft.lng(), topRight.lng()),
                        Math.max(bottomLeft.lng(), bottomRight.lng()));
    }
}
