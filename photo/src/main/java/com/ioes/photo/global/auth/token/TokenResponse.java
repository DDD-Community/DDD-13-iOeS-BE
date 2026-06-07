package com.ioes.photo.global.auth.token;

import com.ioes.photo.global.auth.oauth.OAuthProvider;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 로그인/토큰 갱신 응답 DTO
 *
 * @param accessToken 서버 발급 Access Token
 * @param refreshToken 서버 발급 Refresh Token
 * @param profile 사용자 프로필 정보
 * @author 황제연
 */
@Schema(description = "로그인/토큰 갱신 응답")
public record TokenResponse(
    @Schema(description = "서버 발급 Access Token") String accessToken,
    @Schema(description = "서버 발급 Refresh Token") String refreshToken,
    @Schema(description = "사용자 프로필 (토큰 갱신 시 null)") UserProfile profile
) {

    public static TokenResponse ofTokenOnly(String accessToken, String refreshToken) {
        return new TokenResponse(accessToken, refreshToken, null);
    }

    @Schema(description = "사용자 프로필 정보")
    public record UserProfile(
        @Schema(description = "사용자 ID") String userId,
        @Schema(description = "이메일") String email,
        @Schema(description = "닉네임") String nickname,
        @Schema(description = "프로필 이미지 URL") String profileImageUrl,
        @Schema(description = "소셜 로그인 제공자 (KAKAO/APPLE)") OAuthProvider provider
    ) {}
}
