package com.ioes.photo.domain.user.dto;

import com.ioes.photo.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 프로필 업데이트 응답 DTO.
 *
 * @param displayName     닉네임#해시태그 형식의 표시 이름
 * @param profileImageUrl 프로필 이미지 URL
 * @author 황제연
 */
@Schema(description = "프로필 업데이트 응답")
public record UpdateProfileResponse(
    @Schema(description = "닉네임#해시태그 형식의 표시 이름") String displayName,
    @Schema(description = "프로필 이미지 URL") String profileImageUrl
) {
    public static UpdateProfileResponse from(User user, String resolvedProfileImageUrl) {
        return new UpdateProfileResponse(
            user.getDisplayName(),
            resolvedProfileImageUrl
        );
    }
}
