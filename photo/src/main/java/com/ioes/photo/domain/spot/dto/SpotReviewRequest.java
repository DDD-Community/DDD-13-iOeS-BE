package com.ioes.photo.domain.spot.dto;

import com.ioes.photo.domain.spot.enums.RejectionReason;
import com.ioes.photo.domain.spot.enums.ReviewDecision;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 스팟 검수(승인/반려) 요청 DTO.
 *
 * 승인 시 decision=APPROVED 만 전달하고, 반려 시 decision=REJECTED 와 reason 을 함께 전달한다.
 * reason 이 ETC(기타)인 경우 detail 이 필수이며, 상세 검증은 서비스에서 수행한다.
 *
 * @author 황제연
 */
@Schema(description = "스팟 검수(승인/반려) 요청")
public record SpotReviewRequest(
    @Schema(description = "검수 결정 (APPROVED=승인, REJECTED=반려)", example = "REJECTED")
    @NotNull
    ReviewDecision decision,

    @Schema(description = "반려 사유 코드 (반려 시 필수). DUPLICATE, LOW_QUALITY, LOCATION_MISMATCH, FILTER_MISMATCH, ETC", example = "LOW_QUALITY")
    RejectionReason reason,

    @Schema(description = "반려 사유 상세 설명 (reason=ETC 인 경우 필수)")
    String detail
) {}
