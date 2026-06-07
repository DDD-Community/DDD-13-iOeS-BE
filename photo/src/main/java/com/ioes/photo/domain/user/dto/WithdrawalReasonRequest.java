package com.ioes.photo.domain.user.dto;

import com.ioes.photo.domain.user.enums.WithdrawalReasonType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 회원탈퇴 사유 등록 요청 DTO.
 *
 * @param reasonType 탈퇴 사유 유형
 * @param content    기타 사유 내용 (OTHERS일 때만 작성, 최대 200자)
 * @author 황제연
 */
@Schema(description = "회원탈퇴 사유 등록 요청")
public record WithdrawalReasonRequest(
    @Schema(description = "탈퇴 사유 유형") @NotNull WithdrawalReasonType reasonType,
    @Schema(description = "기타 사유 내용 (최대 200자)") @Size(max = 200, message = "내용은 200자 이하로 입력해주세요.") String content
) {}
