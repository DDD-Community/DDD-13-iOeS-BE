package com.ioes.photo.global.auth.oauth.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Kakao 토큰 엔드포인트 응답 DTO
 *
 * @param tokenType 토큰 타입 (항상 "bearer")
 * @param accessToken Kakao access token
 * @param expiresIn access token 만료 시간 (초)
 * @param refreshToken Kakao refresh token
 * @param refreshTokenExpiresIn refresh token 만료 시간 (초)
 * @author 황제연
 */
public record KakaoTokenResponse(
    @JsonProperty("token_type")
    String tokenType,
    @JsonProperty("access_token")
    String accessToken,
    @JsonProperty("expires_in")
    int expiresIn,
    @JsonProperty("refresh_token")
    String refreshToken,
    @JsonProperty("refresh_token_expires_in")
    int refreshTokenExpiresIn
) {
}
