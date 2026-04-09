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

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteAccount(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        userService.deleteAccount(userId);
        return ApiResponse.success();
    }
}
