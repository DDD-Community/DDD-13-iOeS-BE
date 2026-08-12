package com.ioes.photo.domain.myspot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 스팟 공개 해제(오픈 신청 철회 / 비공개 전환) 응답 DTO.
 *
 * 두 동작의 결과 상태는 모두 DRAFT 이므로, 클라이언트는 previousStatus 로 안내 문구를 구분한다.
 *
 * @author 황제연
 */
@Schema(description = "스팟 공개 해제 응답")
public record CancelPublicationResponse(
    @Schema(description = "스팟 ID") Long spotId,
    @Schema(description = "해제 전 상태 (PENDING/RE_REVIEW_PENDING=오픈 신청 철회, PUBLISHED=비공개 전환)",
        example = "PUBLISHED") String previousStatus,
    @Schema(description = "해제 후 상태 (항상 DRAFT)", example = "DRAFT") String status
) {}
