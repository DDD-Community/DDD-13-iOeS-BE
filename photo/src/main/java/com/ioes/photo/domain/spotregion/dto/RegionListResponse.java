package com.ioes.photo.domain.spotregion.dto;

import com.ioes.photo.domain.spotregion.entity.SpotRegion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 활성화된 지역 목록 응답 DTO.
 *
 * @author 황제연
 */
@Schema(description = "활성화된 지역 목록 응답")
public record RegionListResponse(
    @Schema(description = "지역 목록") List<RegionItem> regions
) {

    public static RegionListResponse from(List<SpotRegion> regions) {
        return new RegionListResponse(regions.stream().map(RegionItem::from).toList());
    }

    @Schema(description = "지역 항목")
    public record RegionItem(
        @Schema(description = "지역 코드") Long regionId,
        @Schema(description = "지역명") String regionName
    ) {

        public static RegionItem from(SpotRegion region) {
            return new RegionItem(region.getRegionId(), region.getRegionName());
        }
    }
}
