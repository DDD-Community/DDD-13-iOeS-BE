package com.ioes.photo.global.auth.oauth.apple;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Apple 토큰 엔드포인트 응답 DTO.
 *
 * @param accessToken Apple access token
 * @param tokenType 토큰 타입 (항상 "bearer")
 * @param expiresIn access token 만료 시간 (초)
 * @param refreshToken Apple refresh token
 * @param idToken 사용자 정보가 담긴 ID token (JWT)
 * @author 황제연
 */
public record AppleTokenResponse(
    @JsonProperty("access_token")
    String accessToken,
    @JsonProperty("token_type")
    String tokenType,
    @JsonProperty("expires_in")
    int expiresIn,
    @JsonProperty("refresh_token")
    String refreshToken,
    @JsonProperty("id_token")
    String idToken
) {
}