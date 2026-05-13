package com.ioes.photo.domain.user.controller;

import com.ioes.photo.domain.user.dto.UpdateProfileRequest;
import com.ioes.photo.domain.user.dto.UpdateProfileResponse;
import com.ioes.photo.domain.user.service.UserProfileService;
import com.ioes.photo.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 사용자 계정 관리 컨트롤러.
 *
 * @author 황제연
 */
@Tag(name = "사용자", description = "사용자 계정 관리 API")
@Validated
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userService;

    @Operation(summary = "프로필 수정", description = "닉네임·이메일·프로필 이미지를 선택적으로 수정합니다. null인 필드는 변경되지 않습니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UpdateProfileResponse> updateProfile(
        Authentication authentication,
        @Parameter(description = "변경할 닉네임 (1~20자)")
        @RequestParam(required = false)
        @Size(min = 1, max = 20, message = "닉네임은 1~20자여야 합니다.")
        String nickname,
        @Parameter(description = "등록할 이메일 (한 번 등록하면 변경 불가)")
        @RequestParam(required = false)
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,
        @Parameter(description = "프로필 이미지 파일")
        @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        Long userId = Long.parseLong(authentication.getName());
        UpdateProfileRequest request = new UpdateProfileRequest(nickname, email);
        return ApiResponse.success(userService.updateProfile(userId, request, profileImage));
    }

    @Operation(summary = "회원 탈퇴", description = "계정을 소프트 삭제하고 모든 토큰 및 OAuth 연동을 해제합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/me")
    public ApiResponse<Void> deleteAccount(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        userService.deleteAccount(userId);
        return ApiResponse.success();
    }
}
