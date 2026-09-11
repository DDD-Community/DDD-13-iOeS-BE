package com.ioes.photo.domain.spot.dto;

import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.mapper.AdminSpotRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 어드민 검수 목록 조회 응답 DTO.
 *
 * @author 황제연
 */
@Schema(description = "어드민 검수 목록 조회 응답")
public record AdminSpotListResponse(
    @Schema(description = "검수 대상 스팟 목록") List<AdminSpotItem> items,
    @Schema(description = "현재 페이지 번호 (0부터 시작)") int page,
    @Schema(description = "다음 페이지 존재 여부") boolean hasNext
) {

    @Schema(description = "검수 대상 스팟 항목")
    public record AdminSpotItem(
        @Schema(description = "스팟 ID") Long id,
        @Schema(description = "오픈 신청 일시") LocalDateTime appliedAt,
        @Schema(description = "등록 유저 닉네임") String userNickname,
        @Schema(description = "스팟명") String name,
        @Schema(description = "상태 (PENDING/RE_REVIEW_PENDING/PUBLISHED/REJECTED)") String status,
        @Schema(description = "처리자 닉네임 (미처리면 null)") String handlerName,
        @Schema(description = "처리 일시 (미처리면 null)") LocalDateTime handledAt
    ) {
        public static AdminSpotItem from(AdminSpotRow row) {
            return new AdminSpotItem(
                row.id(),
                row.appliedAt(),
                row.userNickname(),
                row.name(),
                SpotStatus.fromCode(row.status()).name(),
                row.handlerName(),
                row.handledAt()
            );
        }
    }
}
