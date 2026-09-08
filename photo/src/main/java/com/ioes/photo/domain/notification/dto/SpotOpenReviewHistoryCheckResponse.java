package com.ioes.photo.domain.notification.dto;

import com.ioes.photo.domain.notification.entity.SpotOpenReviewHistory;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 검수완료 히스토리 확인여부 업데이트 응답 DTO.
 *
 * @author 황제연
 */
@Schema(description = "검수완료 히스토리 확인여부 업데이트 응답")
public record SpotOpenReviewHistoryCheckResponse(
    @Schema(description = "히스토리 ID") Long historyId,
    @Schema(description = "확인 여부 (Y/N)", example = "Y") String checkYn
) {
    public static SpotOpenReviewHistoryCheckResponse from(SpotOpenReviewHistory history) {
        return new SpotOpenReviewHistoryCheckResponse(history.getId(), history.getCheckYn().getCode());
    }
}
