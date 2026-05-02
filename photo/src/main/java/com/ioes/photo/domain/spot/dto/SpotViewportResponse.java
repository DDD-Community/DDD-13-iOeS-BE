package com.ioes.photo.domain.spot.dto;

import java.util.List;

/**
 * 뷰포트 내 스팟 목록 응답.
 *
 * @author 황제연
 */
public record SpotViewportResponse(List<SpotSummary> spots) {

    public record SpotSummary(
        Long spotId,
        String spotImageUrl,
        Double latitude,
        Double longitude
    ) {}
}
