package com.ioes.photo.domain.spot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 어드민 검수 상세 조회 응답 DTO.
 *
 * 유저가 등록한 내용 전체 + 과거 반려 이력 + 등록 유저 신뢰도 정보를 포함한다.
 *
 * @author 황제연
 */
@Schema(description = "어드민 검수 상세 조회 응답")
public record AdminSpotDetailResponse(
    @Schema(description = "스팟 ID") Long id,
    @Schema(description = "스팟명") String name,
    @Schema(description = "등록 유저 닉네임") String userNickname,
    @Schema(description = "상태 (PENDING/RE_REVIEW_PENDING/PUBLISHED/REJECTED)") String status,
    @Schema(description = "오픈 신청 일시") LocalDateTime appliedAt,
    @Schema(description = "사진 URL 목록 (미승인 건은 만료되는 presigned URL)") List<String> photoUrls,
    @Schema(description = "상세 주소") String address,
    @Schema(description = "한줄 코멘트") String comment,
    @Schema(description = "촬영 일시") LocalDateTime shotAt,
    @Schema(description = "테마 코드 (SUNSET/YUNSEUL)") String theme,
    @Schema(description = "테마 한글명") String themeLabel,
    @Schema(description = "과거 반려 이력 (최신순)") List<RejectionHistoryItem> rejectionHistory,
    @Schema(description = "등록 유저 신뢰도 정보") UserTrust userTrust
) {

    @Schema(description = "반려 이력 항목")
    public record RejectionHistoryItem(
        @Schema(description = "반려 사유 코드") String reason,
        @Schema(description = "반려 사유 한글명") String reasonLabel,
        @Schema(description = "상세 설명 (기타 사유인 경우에만 채워짐)") String detail,
        @Schema(description = "처리자 닉네임") String handlerName,
        @Schema(description = "반려 일시") LocalDateTime rejectedAt
    ) {}

    @Schema(description = "등록 유저 신뢰도 정보")
    public record UserTrust(
        @Schema(description = "가입일") LocalDateTime joinedAt,
        @Schema(description = "누적 등록 스팟 수") long totalRegistered,
        @Schema(description = "누적 승인 스팟 수") long totalApproved,
        @Schema(description = "누적 반려 스팟 수") long totalRejected
    ) {}
}
