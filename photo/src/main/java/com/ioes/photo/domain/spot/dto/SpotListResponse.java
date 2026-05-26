package com.ioes.photo.domain.spot.dto;

import com.ioes.photo.global.common.annotation.TruncateDecimal;
import java.util.List;

/**
 * 스팟 리스트 조회 응답 DTO.
 *
 * @author 황제연
 */
public record SpotListResponse(
    List<SpotItem> spots,
    int page,
    boolean hasNext
) {

    public record SpotItem(
        Long spotId,
        String name,
        String theme,
        String thumbnailUrl,
        @TruncateDecimal Double distanceKm,
        boolean isBookmarked
    ) {}
}