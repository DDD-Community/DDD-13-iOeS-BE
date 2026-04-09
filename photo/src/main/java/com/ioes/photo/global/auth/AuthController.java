package com.ioes.photo.global.auth;

import com.ioes.photo.global.auth.oauth.OAuthProvider;
import com.ioes.photo.global.auth.oauth.OAuthService;
import com.ioes.photo.global.auth.token.TokenResponse;
import com.ioes.photo.global.auth.token.TokenService;
import com.ioes.photo.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * OAuth 인증 및 토큰 관리 컨트롤러.
 *
 * @author 황제연
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OAuthService oAuthService;
    private final TokenService tokenService;

    @GetMapping("/oauth/{provider}/authorize")
    public ApiResponse<Map<String, String>> getAuthorizationUrl(@PathVariable String provider) {
        OAuthProvider oAuthProvider = oAuthService.resolveProvider(provider);
        String url = oAuthService.getAuthorizationUrl(oAuthProvider);
        return ApiResponse.success(Map.of("authorizationUrl", url));
    }

    @GetMapping("/oauth/{provider}/callback")
    public ApiResponse<TokenResponse> oauthCallback(
        @PathVariable String provider,
        @RequestParam Map<String, String> params
    ) {
        OAuthProvider oAuthProvider = oAuthService.resolveProvider(provider);
        return ApiResponse.success(oAuthService.handleCallback(oAuthProvider, params));
    }

    @PostMapping("/oauth/apple/callback")
    public ApiResponse<TokenResponse> appleCallback(@RequestParam Map<String, String> params) {
        return ApiResponse.success(oAuthService.handleCallback(OAuthProvider.APPLE, params));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        String[] tokens = tokenService.refreshTokens(refreshToken);
        return ApiResponse.success(TokenResponse.ofTokenOnly(tokens[0], tokens[1]));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody Map<String, String> body) {
        tokenService.invalidateRefreshToken(body.get("refreshToken"));
        return ApiResponse.success();
    }
}