package com.ioes.photo.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 프로필 업데이트 요청 DTO.
 *
 * null인 필드는 변경하지 않습니다.
 *
 * @param nickname 변경할 닉네임 (null이면 유지)
 * @author 황제연
 */
@Schema(description = "프로필 업데이트 요청")
public record UpdateProfileRequest(
    @Schema(description = "변경할 닉네임 (2~12자, 한글/영문/숫자, null이면 유지)") String nickname
) {}
