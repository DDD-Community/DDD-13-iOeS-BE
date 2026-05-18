package com.ioes.photo.domain.spot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 스팟 어드민 배치 등록 응답 DTO.
 *
 * @author 황제연
 */
@Schema(description = "스팟 배치 등록 응답")
public record SpotAdminCreateResponse(
    @Schema(description = "등록된 스팟 목록") List<SpotResult> created,
    @Schema(description = "등록된 스팟 수") int count
) {

    @Schema(description = "등록된 스팟 요약")
    public record SpotResult(
        @Schema(description = "스팟 ID") Long spotId,
        @Schema(description = "스팟 이름") String name
    ) {}

    public static SpotAdminCreateResponse of(List<SpotResult> created) {
        return new SpotAdminCreateResponse(created, created.size());
    }
}
