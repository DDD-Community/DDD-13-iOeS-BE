package com.ioes.photo.global.auth.oauth.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Kakao 사용자 정보 API 응답 DTO
 *
 * @param id Kakao 고유 사용자 ID
 * @param kakaoAccount 카카오 계정 정보 (이메일, 프로필 등)
 * @author 황제연
 */
public record KakaoUserInfoResponse(
    Long id,
    @JsonProperty("kakao_account")
    KakaoAccount kakaoAccount
) {

    public record KakaoAccount(
        String email,
        @JsonProperty("email_verified")
        Boolean emailVerified,
        Profile profile
    ) {}

    public record Profile(
        String nickname,
        @JsonProperty("profile_image_url")
        String profileImageUrl
    ) {}
}
