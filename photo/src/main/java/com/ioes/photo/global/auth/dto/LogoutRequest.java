package com.ioes.photo.global.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그아웃 요청 DTO.
 *
 * @param refreshToken 무효화할 Refresh Token
 * @author 황제연
 */
@Schema(description = "로그아웃 요청")
public record LogoutRequest(
    @Schema(description = "무효화할 Refresh Token")
    @NotBlank(message = "refreshToken은 필수입니다.")
    String refreshToken
) {}