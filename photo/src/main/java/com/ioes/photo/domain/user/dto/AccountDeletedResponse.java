package com.ioes.photo.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 탈퇴 계정 소셜 로그인 시도 응답 DTO.
 *
 * 클라이언트는 restoreToken을 사용해 PATCH /v1/users/restore 를 호출하여 계정을 복구할 수 있다.
 *
 * @author 황제연
 */
@Schema(description = "탈퇴 계정 로그인 시도 응답")
public record AccountDeletedResponse(
    @Schema(description = "계정 복구용 일회성 토큰 (5분 유효)") String restoreToken
) {}
