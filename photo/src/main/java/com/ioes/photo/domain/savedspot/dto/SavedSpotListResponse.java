package com.ioes.photo.domain.savedspot.dto;

import com.ioes.photo.global.common.annotation.TruncateDecimal;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 저장된 스팟 목록 조회 응답 DTO.
 *
 * @author 황제연
 */
@Schema(description = "저장된 스팟 목록 응답")
public record SavedSpotListResponse(
    @Schema(description = "저장된 스팟 목록") List<SavedSpotItem> spots,
    @Schema(description = "현재 페이지 번호 (0-based)") int page,
    @Schema(description = "다음 페이지 존재 여부") boolean hasNext
) {

    @Schema(description = "저장된 스팟 항목")
    public record SavedSpotItem(
        @Schema(description = "스팟 ID") Long spotId,
        @Schema(description = "스팟 이름") String name,
        @Schema(description = "스팟 테마 코드") String theme,
        @Schema(description = "원본 이미지 URL (없으면 null)") String imageUrl,
        @Schema(description = "스팟 위도") Double latitude,
        @Schema(description = "스팟 경도") Double longitude,
        @Schema(description = "사용자 위치 기준 거리 (km), 위치 미전달 시 null") @TruncateDecimal Double distanceKm,
        @Schema(description = "북마크 수") long bookmarkCount,
        @Schema(description = "북마크 저장 시각") LocalDateTime savedAt,
        @Schema(description = "스팟 삭제 여부") boolean deleted
    ) {}
}
