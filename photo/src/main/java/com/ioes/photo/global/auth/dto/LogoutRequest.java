package com.ioes.photo.global.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그아웃 요청 DTO.
 *
 * @param refreshToken 무효화할 Refresh Token
 * @author 황제연
 */
public record LogoutRequest(
    @NotBlank(message = "refreshToken은 필수입니다.")
    String refreshToken
) {}