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
        @Schema(description = "원본 이미지 URL (비공개 전환된 스팟이거나 이미지가 없으면 null)") String imageUrl,
        @Schema(description = "스팟 위도") Double latitude,
        @Schema(description = "스팟 경도") Double longitude,
        @Schema(description = "사용자 위치 기준 거리 (km), 위치 미전달 시 null") @TruncateDecimal Double distanceKm,
        @Schema(description = "북마크 수") long bookmarkCount,
        @Schema(description = "좋아요(추천) 수") long likeCount,
        @Schema(description = "북마크 저장 시각") LocalDateTime savedAt,
        @Schema(description = "스팟 삭제 여부") boolean deleted,
        // status(PUBLISHED 여부)만 반영하고 rel_yn은 반영하지 않는 레거시 필드다. isReleased가 rel_yn까지
        // 포함한 정확한 노출 여부를 알려주므로, 프론트가 isReleased로 전환하면 이 필드는 폐기 대상이다.
        @Schema(description = "비공개 여부 (아직 승인되지 않은 상태, PUBLISHED가 아니면 true). "
            + "rel_yn은 반영하지 않으며, 정확한 노출 여부는 isReleased를 사용할 것") boolean isPrivate,
        @Schema(description = "노출 여부 (검수 상태와 별개로 등록자가 지도뷰/리스트 노출을 껐는지, true=노출, false=비노출)") boolean isReleased
    ) {}
}
