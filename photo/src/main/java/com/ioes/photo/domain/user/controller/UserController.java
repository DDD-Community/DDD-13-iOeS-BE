package com.ioes.photo.domain.user.controller;

import com.ioes.photo.domain.user.dto.MypageHomeResponse;
import com.ioes.photo.domain.user.dto.UpdateProfileRequest;
import com.ioes.photo.domain.user.dto.UpdateProfileResponse;
import com.ioes.photo.domain.user.dto.WithdrawalReasonRequest;
import com.ioes.photo.domain.user.service.UserProfileService;
import com.ioes.photo.domain.user.service.WithdrawalReasonService;
import com.ioes.photo.global.auth.CurrentUserId;
import com.ioes.photo.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final WithdrawalReasonService withdrawalReasonService;

    @Operation(summary = "마이페이지 홈탭 조회", description = "프로필 이미지, 닉네임, 저장한 스팟 수, 등록한 스팟 수를 반환합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/me")
    public ApiResponse<MypageHomeResponse> getMyPageHome(@CurrentUserId Long userId) {
        return ApiResponse.success(userService.getMyPageHome(userId));
    }

    @Operation(summary = "프로필 수정", description = "닉네임·프로필 이미지를 선택적으로 수정합니다. null인 필드는 변경되지 않습니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UpdateProfileResponse> updateProfile(
        @CurrentUserId Long userId,
        @Parameter(description = "변경할 닉네임 (2~12자, 한글/영문/숫자)")
        @RequestParam(required = false)
        @Size(min = 2, max = 12, message = "닉네임은 12자 이하로 입력해주세요.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 사용 가능해요.")
        String nickname,
        @Parameter(description = "프로필 이미지 파일")
        @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        return ApiResponse.success(userService.updateProfile(userId, new UpdateProfileRequest(nickname), profileImage));
    }

    @Operation(summary = "회원 탈퇴", description = "계정을 소프트 삭제하고 모든 토큰 및 OAuth 연동을 해제합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/me")
    public ApiResponse<Void> deleteAccount(@CurrentUserId Long userId) {
        userService.deleteAccount(userId);
        return ApiResponse.success();
    }

    @Operation(summary = "탈퇴 계정 복구", description = "소셜 로그인 탈퇴 감지 시 발급된 restoreToken으로 계정을 복구합니다. 복구 후 소셜 로그인을 재시도해야 합니다.")
    @SecurityRequirements
    @PatchMapping("/restore")
    public ApiResponse<Void> restoreAccount(
        @Parameter(description = "계정 복구용 일회성 토큰")
        @RequestParam @NotBlank String restoreToken
    ) {
        userService.restoreAccount(restoreToken);
        return ApiResponse.success();
    }

    @Operation(summary = "탈퇴 사유 등록", description = "회원탈퇴 사유를 등록합니다. 기타 사유일 때만 내용을 작성할 수 있습니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/me/withdrawal-reason")
    public ApiResponse<Void> saveWithdrawalReason(
        @CurrentUserId Long userId,
        @Valid @RequestBody WithdrawalReasonRequest request
    ) {
        withdrawalReasonService.saveWithdrawalReason(userId, request);
        return ApiResponse.success();
    }
}
