package com.ioes.photo.global.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 카카오 네이티브 SDK 로그인 요청 DTO.
 *
 * @author 황제연
 */
@Schema(description = "카카오 네이티브 SDK 로그인 요청")
public record KakaoLoginRequest(
    @Schema(description = "Kakao SDK로 발급받은 액세스 토큰", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "카카오 액세스 토큰은 필수입니다.")
    String accessToken
) {}
