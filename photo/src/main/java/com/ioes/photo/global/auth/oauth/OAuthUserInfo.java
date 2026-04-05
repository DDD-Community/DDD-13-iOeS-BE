package com.ioes.photo.global.auth.oauth;

/**
 * OAuth 공급자에서 추출한 사용자 정보 공통 DTO
 *
 * @param providerId 공급자가 발급한 고유 사용자 ID (sub 또는 id)
 * @param email 이메일 (공급자가 제공하지 않을 수 있음)
 * @param nickname 닉네임 (공급자가 제공하지 않을 수 있음)
 * @param profileImageUrl 프로필 이미지 URL (공급자가 제공하지 않을 수 있음)
 * @param provider OAuth 공급자
 * @author 황제연
 */
public record OAuthUserInfo(
    String providerId,
    String email,
    String nickname,
    String profileImageUrl,
    OAuthProvider provider
) {
}