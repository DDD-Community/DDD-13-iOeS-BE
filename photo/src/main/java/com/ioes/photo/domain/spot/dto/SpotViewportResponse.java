package com.ioes.photo.domain.spot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 뷰포트 내 스팟 목록 응답.
 *
 * @author 황제연
 */
@Schema(description = "뷰포트 내 스팟 목록 응답")
public record SpotViewportResponse(
    @Schema(description = "스팟 목록") List<SpotSummary> spots
) {

    @Schema(description = "스팟 요약 정보")
    public record SpotSummary(
        @Schema(description = "스팟 ID") Long spotId,
        @Schema(description = "썸네일 이미지 URL (없으면 null)") String spotImageUrl,
        @Schema(description = "위도") Double latitude,
        @Schema(description = "경도") Double longitude,
        @Schema(description = "내 스팟 여부, 비로그인 시 false") boolean isMySpot
    ) {}
}
