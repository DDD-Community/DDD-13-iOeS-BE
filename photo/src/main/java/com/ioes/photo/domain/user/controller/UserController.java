package com.ioes.photo.domain.user.controller;

import com.ioes.photo.domain.user.dto.UpdateProfileRequest;
import com.ioes.photo.domain.user.dto.UpdateProfileResponse;
import com.ioes.photo.domain.user.service.UserService;
import com.ioes.photo.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
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
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UpdateProfileResponse> updateProfile(
        Authentication authentication,
        @RequestParam(required = false) String nickname,
        @RequestParam(required = false) String email,
        @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        Long userId = Long.parseLong(authentication.getName());
        UpdateProfileRequest request = new UpdateProfileRequest(nickname, email);
        return ApiResponse.success(userService.updateProfile(userId, request, profileImage));
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteAccount(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        userService.deleteAccount(userId);
        return ApiResponse.success();
    }
}
