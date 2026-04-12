package com.ioes.photo.global.auth;

import com.ioes.photo.global.auth.dto.LogoutRequest;
import com.ioes.photo.global.auth.dto.RefreshRequest;
import com.ioes.photo.global.auth.oauth.OAuthProvider;
import com.ioes.photo.global.auth.oauth.OAuthService;
import com.ioes.photo.global.auth.token.TokenResponse;
import com.ioes.photo.global.auth.token.TokenService;
import com.ioes.photo.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@Tag(name = "인증", description = "OAuth 소셜 로그인 및 토큰 관리 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OAuthService oAuthService;
    private final TokenService tokenService;

    @Operation(
        summary = "인증 URL 조회",
        description = "소셜 로그인 페이지로 이동할 인증 URL을 반환합니다. State(CSRF 방지)와 PKCE(code 가로채기 방지)가 자동으로 포함됩니다."
    )
    @SecurityRequirements
    @GetMapping("/oauth/{provider}/authorize")
    public ApiResponse<Map<String, String>> getAuthorizationUrl(
        @Parameter(description = "OAuth 공급자 (KAKAO | APPLE)", required = true)
        @PathVariable String provider
    ) {
        OAuthProvider oAuthProvider = oAuthService.resolveProvider(provider);
        String url = oAuthService.getAuthorizationUrl(oAuthProvider);
        return ApiResponse.success(Map.of("authorizationUrl", url));
    }

    @Operation(
        summary = "OAuth 콜백 처리 — GET",
        description = "Kakao 등 GET 방식 콜백을 처리합니다. code와 state가 쿼리 파라미터로 전달됩니다."
    )
    @SecurityRequirements
    @GetMapping("/oauth/{provider}/callback")
    public ApiResponse<TokenResponse> oauthCallback(
        @Parameter(description = "OAuth 공급자 (kakao)", required = true)
        @PathVariable String provider,
        @RequestParam Map<String, String> params
    ) {
        OAuthProvider oAuthProvider = oAuthService.resolveProvider(provider);
        return ApiResponse.success(oAuthService.handleCallback(oAuthProvider, params));
    }

    @Operation(
        summary = "Apple OAuth 콜백 처리 — POST",
        description = "Apple Sign In의 response_mode=form_post 방식 콜백을 처리합니다."
    )
    @SecurityRequirements
    @PostMapping("/oauth/apple/callback")
    public ApiResponse<TokenResponse> appleCallback(@RequestParam Map<String, String> params) {
        return ApiResponse.success(oAuthService.handleCallback(OAuthProvider.APPLE, params));
    }

    @Operation(
        summary = "토큰 갱신",
        description = "Refresh Token으로 새 Access Token과 Refresh Token을 발급합니다. (Refresh Token Rotation)"
    )
    @SecurityRequirements
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        String[] tokens = tokenService.refreshTokens(request.refreshToken());
        return ApiResponse.success(TokenResponse.ofTokenOnly(tokens[0], tokens[1]));
    }

    @Operation(
        summary = "로그아웃",
        description = "Refresh Token을 무효화합니다. Access Token은 만료까지 유효합니다."
    )
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        tokenService.invalidateRefreshToken(request.refreshToken());
        return ApiResponse.success();
    }
}
