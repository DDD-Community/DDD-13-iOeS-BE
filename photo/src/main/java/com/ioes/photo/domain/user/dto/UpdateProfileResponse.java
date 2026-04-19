package com.ioes.photo.domain.user.dto;

import com.ioes.photo.domain.user.entity.User;

/**
 * 프로필 업데이트 응답 DTO.
 *
 * @param displayName     닉네임#해시태그 형식의 표시 이름
 * @param email           이메일 주소
 * @param profileImageUrl 프로필 이미지 URL
 * @author 황제연
 */
public record UpdateProfileResponse(
    String displayName,
    String email,
    String profileImageUrl
) {
    public static UpdateProfileResponse from(User user, String resolvedProfileImageUrl) {
        return new UpdateProfileResponse(
            user.getDisplayName(),
            user.getEmail(),
            resolvedProfileImageUrl
        );
    }
}