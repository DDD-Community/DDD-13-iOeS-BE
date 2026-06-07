package com.ioes.photo.domain.myspot.dto;

import com.ioes.photo.global.common.annotation.TruncateDecimal;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 나만의 스팟 목록 조회 응답 DTO.
 *
 * @author 김성민
 */
@Schema(description = "나만의 스팟 목록 응답")
public record MySpotListResponse(
    @Schema(description = "나만의 스팟 목록") List<MySpotItem> spots,
    @Schema(description = "현재 페이지 번호 (0-based)") int page,
    @Schema(description = "다음 페이지 존재 여부") boolean hasNext
) {

    @Schema(description = "나만의 스팟 항목")
    public record MySpotItem(
        @Schema(description = "스팟 ID") Long spotId,
        @Schema(description = "스팟 이름") String name,
        @Schema(description = "스팟 테마 코드") String theme,
        @Schema(description = "원본 이미지 URL (없으면 null)") String imageUrl,
        @Schema(description = "스팟 위도") Double latitude,
        @Schema(description = "스팟 경도") Double longitude,
        @Schema(description = "사용자 위치 기준 거리 (km), 위치 미전달 시 null") @TruncateDecimal Double distanceKm,
        @Schema(description = "스팟 등록 시각") LocalDateTime createdAt,
        @Schema(description = "스팟 상태 (PENDING=검수대기, PUBLISHED=공개, REJECTED=반려)") String status,
        @Schema(description = "북마크 수") long bookmarkCount
    ) {}
}
