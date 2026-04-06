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

    /*
     * OAuth 인증 URL을 반환합니다.
     *
     * @param provider OAuth 공급자 (apple, kakao - 대소문자 무관)
     * @return 인증 페이지 URL을 담은 응답
     */
    @GetMapping("/oauth/{provider}/authorize")
    public ApiResponse<Map<String, String>> getAuthorizationUrl(@PathVariable String provider) {
        OAuthProvider oAuthProvider = oAuthService.resolveProvider(provider);
        String url = oAuthService.getAuthorizationUrl(oAuthProvider);
        return ApiResponse.success(Map.of("authorizationUrl", url));
    }

    /*
     * GET 방식 OAuth 콜백을 처리합니다 (Kakao 등 표준 OAuth 공급자).
     *
     * 새로운 GET 기반 공급자(Google, Naver 등)를 추가해도 이 엔드포인트를 재사용합니다.
     *
     * @param provider OAuth 공급자 이름
     * @param params 공급자가 전달한 콜백 파라미터 (code 등)
     * @return 발급된 토큰 및 프로필 정보
     */
    @GetMapping("/oauth/{provider}/callback")
    public ApiResponse<TokenResponse> oauthCallback(
        @PathVariable String provider,
        @RequestParam Map<String, String> params
    ) {
        OAuthProvider oAuthProvider = oAuthService.resolveProvider(provider);
        return ApiResponse.success(oAuthService.handleCallback(oAuthProvider, params));
    }

    /*
     * Apple 인증 콜백을 처리합니다 (POST, form_post 방식).
     *
     * <p>Apple은 response_mode=form_post로 인증 코드를 POST로 전송하므로 별도 엔드포인트를 유지합니다
     */
    @PostMapping("/oauth/apple/callback")
    public ApiResponse<TokenResponse> appleCallback(@RequestParam Map<String, String> params) {
        return ApiResponse.success(oAuthService.handleCallback(OAuthProvider.APPLE, params));
    }

    /*
     * Refresh Token으로 새 Access Token과 Refresh Token을 발급합니다.
     */
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        String[] tokens = tokenService.refreshTokens(refreshToken);
        return ApiResponse.success(TokenResponse.ofTokenOnly(tokens[0], tokens[1]));
    }

    /*
     * Refresh Token을 무효화합니다 (로그아웃).
     *
     * @param body {@code {"refreshToken": "..."}} 형태의 요청 바디
     * @return 성공 응답
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody Map<String, String> body) {
        tokenService.invalidateRefreshToken(body.get("refreshToken"));
        return ApiResponse.success();
    }
}