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

    /**
     * 카카오 계정 정보
     *
     * @param email 이메일 (동의 시 제공)
     * @param emailVerified 이메일 인증 여부
     * @param profile 프로필 정보
     */
    public record KakaoAccount(
        String email,
        @JsonProperty("email_verified")
        Boolean emailVerified,
        Profile profile
    ) {}

    /**
     * 프로필 정보
     *
     * @param nickname 닉네임
     * @param profileImageUrl 프로필 이미지 URL
     */
    public record Profile(
        String nickname,
        @JsonProperty("profile_image_url")
        String profileImageUrl
    ) {}
}
