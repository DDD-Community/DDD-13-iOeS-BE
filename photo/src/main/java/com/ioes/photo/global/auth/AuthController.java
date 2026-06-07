package com.ioes.photo.global.auth;

import com.ioes.photo.global.auth.dto.AppleLoginRequest;
import com.ioes.photo.global.auth.dto.KakaoLoginRequest;
import com.ioes.photo.global.auth.dto.LogoutRequest;
import com.ioes.photo.global.auth.dto.RefreshRequest;
import com.ioes.photo.global.auth.oauth.OAuthService;
import com.ioes.photo.global.auth.token.TokenResponse;
import com.ioes.photo.global.auth.token.TokenService;
import com.ioes.photo.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 소셜 로그인(네이티브 SDK) 및 토큰 관리 컨트롤러.
 *
 * @author 황제연
 */
@Tag(name = "인증", description = "소셜 로그인(네이티브 SDK) 및 토큰 관리 API")
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OAuthService oAuthService;
    private final TokenService tokenService;

    @Operation(
        summary = "카카오 로그인",
        description = "앱에서 Kakao SDK로 발급받은 액세스 토큰으로 로그인합니다. 자체 JWT(accessToken, refreshToken)를 발급합니다."
    )
    @SecurityRequirements
    @PostMapping("/kakao")
    public ApiResponse<TokenResponse> kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
        return ApiResponse.success(oAuthService.loginWithKakao(request.accessToken()));
    }

    @Operation(
        summary = "Apple 로그인",
        description = "앱에서 Apple SDK로 발급받은 identity token으로 로그인합니다. 최초 로그인 시에만 user(이름/이메일) 정보가 전달됩니다."
    )
    @SecurityRequirements
    @PostMapping("/apple")
    public ApiResponse<TokenResponse> appleLogin(@Valid @RequestBody AppleLoginRequest request) {
        return ApiResponse.success(oAuthService.loginWithApple(request));
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
        description = "Refresh Token을 무효화하고 Access Token을 블랙리스트에 등록합니다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request,
                                    HttpServletRequest httpRequest) {
        String accessToken = BearerTokenExtractor.extract(httpRequest);
        tokenService.logout(request.refreshToken(), accessToken);
        return ApiResponse.success();
    }
}
