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
    UserProfile profile,
    boolean isNewUser
) {

    /*
     * 토큰 갱신 시 프로필 없이 생성합니다.
     */
    public static TokenResponse ofTokenOnly(String accessToken, String refreshToken) {
        return new TokenResponse(accessToken, refreshToken, null, false);
    }

    /*
     * 사용자 프로필 정보
     *
     * @param userId 서버 내부 사용자 ID
     * @param email 이메일
     * @param nickname 닉네임
     * @param profileImageUrl 프로필 이미지 URL
     * @param provider OAuth 공급자
     */
    public record UserProfile(
        String userId,
        String email,
        String nickname,
        String profileImageUrl,
        OAuthProvider provider
    ) {}
}
