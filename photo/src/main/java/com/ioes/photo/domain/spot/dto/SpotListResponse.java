package com.ioes.photo.domain.spot.dto;

import com.ioes.photo.global.common.annotation.TruncateDecimal;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 스팟 리스트 조회 응답 DTO.
 *
 * @author 황제연
 */
@Schema(description = "스팟 리스트 조회 응답")
public record SpotListResponse(
    @Schema(description = "스팟 목록") List<SpotItem> spots,
    @Schema(description = "현재 페이지 번호") int page,
    @Schema(description = "다음 페이지 존재 여부") boolean hasNext
) {

    @Schema(description = "스팟 목록 항목")
    public record SpotItem(
        @Schema(description = "스팟 ID") Long spotId,
        @Schema(description = "스팟 이름") String name,
        @Schema(description = "테마 코드 (SS=노을, YS=윤슬, SL=햇살, NV=야경)") String theme,
        @Schema(description = "썸네일 이미지 URL") String thumbnailUrl,
        @Schema(description = "사용자 위치 기준 거리(km), 위치 미전달 시 null") @TruncateDecimal Double distanceKm,
        @Schema(description = "북마크 수") long bookmarkCount,
        @Schema(description = "북마크 여부 (비로그인 시 false)") boolean isBookmarked,
        @Schema(description = "좋아요(추천) 수") long likeCount,
        @Schema(description = "좋아요 여부 (비로그인 시 false)") boolean isLiked
    ) {}
}