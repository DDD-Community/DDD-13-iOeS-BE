package com.ioes.photo.global.auth.token;

import com.ioes.photo.global.auth.oauth.OAuthProvider;

/**
 * 로그인/토큰 갱신 응답 DTO
 *
 * @param accessToken 서버 발급 Access Token
 * @param refreshToken 서버 발급 Refresh Token
 * @param profile 사용자 프로필 정보
 * @author 황제연
 */
public record TokenResponse(
    String accessToken,
    String refreshToken,
    UserProfile profile
) {

    public static TokenResponse ofTokenOnly(String accessToken, String refreshToken) {
        return new TokenResponse(accessToken, refreshToken, null);
    }

    public record UserProfile(
        String userId,
        String email,
        String nickname,
        String profileImageUrl,
        OAuthProvider provider
    ) {}
}
