package com.ioes.photo.domain.user.dto;

import com.ioes.photo.domain.user.entity.User;
import com.ioes.photo.global.auth.oauth.OAuthProvider;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 마이페이지 홈탭 조회 응답 DTO.
 *
 * @param profileImageUrl  프로필 이미지 URL (null이면 기본 이미지)
 * @param nickname         닉네임#해시태그 형식의 표시 이름
 * @param provider         연결된 소셜 계정 공급자 (KAKAO, APPLE)
 * @param savedSpotCount   저장한 스팟 개수
 * @param recordedSpotCount 등록한 스팟 개수 (PENDING 포함)
 * @author 황제연
 */
@Schema(description = "마이페이지 홈탭 조회 응답")
public record MypageHomeResponse(
    @Schema(description = "프로필 이미지 URL") String profileImageUrl,
    @Schema(description = "닉네임#해시태그 형식 표시 이름") String nickname,
    @Schema(description = "연결된 소셜 계정 공급자 (KAKAO, APPLE)") OAuthProvider provider,
    @Schema(description = "저장한 스팟 개수") long savedSpotCount,
    @Schema(description = "등록한 스팟 개수") long recordedSpotCount
) {
    public static MypageHomeResponse of(User user, String profileImageUrl,
                                        long savedSpotCount, long recordedSpotCount) {
        return new MypageHomeResponse(
            profileImageUrl,
            user.getDisplayName(),
            user.getProvider(),
            savedSpotCount,
            recordedSpotCount
        );
    }
}
