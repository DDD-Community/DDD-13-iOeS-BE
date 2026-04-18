package com.ioes.photo.global.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 토큰 갱신 요청 DTO.
 *
 * @param refreshToken 갱신에 사용할 Refresh Token
 * @author 황제연
 */
public record RefreshRequest(
    @NotBlank(message = "refreshToken은 필수입니다.")
    String refreshToken
) {}