package com.ioes.photo.domain.notification.dto;

import com.ioes.photo.domain.notification.entity.SpotOpenReviewHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 미확인 검수완료 히스토리 목록 조회 응답 DTO.
 *
 * 승인/반려 결과를 별도 목록으로 분리해 반환하며, 페이징은 적용하지 않는다.
 *
 * @author 황제연
 */
@Schema(description = "미확인 검수완료 히스토리 목록 조회 응답")
public record SpotOpenReviewHistoryListResponse(
    @Schema(description = "미확인 승인 히스토리 목록") List<ApprovedItem> approved,
    @Schema(description = "미확인 반려 히스토리 목록") List<RejectedItem> rejected
) {

    @Schema(description = "승인 히스토리 항목")
    public record ApprovedItem(
        @Schema(description = "히스토리 ID") Long historyId,
        @Schema(description = "스팟 ID") Long spotId,
        @Schema(description = "검수 처리 일시") LocalDateTime reviewedAt
    ) {
        public static ApprovedItem from(SpotOpenReviewHistory history) {
            return new ApprovedItem(history.getId(), history.getSpotId(), history.getCreatedAt());
        }
    }

    @Schema(description = "반려 히스토리 항목")
    public record RejectedItem(
        @Schema(description = "히스토리 ID") Long historyId,
        @Schema(description = "스팟 ID") Long spotId,
        @Schema(description = "반려 사유 코드") String rejectReason,
        @Schema(description = "반려 사유 한글명") String rejectReasonLabel,
        @Schema(description = "반려 사유 상세 설명") String rejectDetail,
        @Schema(description = "검수 처리 일시") LocalDateTime reviewedAt
    ) {
        public static RejectedItem from(SpotOpenReviewHistory history) {
            return new RejectedItem(
                history.getId(),
                history.getSpotId(),
                history.getRejectReason().name(),
                history.getRejectReason().getLabel(),
                history.getRejectDetail(),
                history.getCreatedAt()
            );
        }
    }
}
