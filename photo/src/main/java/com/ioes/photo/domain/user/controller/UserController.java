package com.ioes.photo.domain.user.controller;

import com.ioes.photo.domain.user.service.UserService;
import com.ioes.photo.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    /**
     * 회원탈퇴.
     *
     * <p>현재 인증된 사용자의 계정을 삭제합니다.
     * 발급된 모든 Refresh Token이 즉시 무효화됩니다.
     *
     * @param authentication Spring Security 인증 객체 (JWT 필터에서 주입)
     * @return 성공 응답
     */
    @DeleteMapping("/me")
    public ApiResponse<Void> deleteAccount(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        userService.deleteAccount(userId);
        return ApiResponse.success();
    }
}
