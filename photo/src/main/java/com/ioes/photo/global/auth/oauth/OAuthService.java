package com.ioes.photo.global.auth.oauth;

import com.ioes.photo.domain.user.entity.User;
import com.ioes.photo.domain.user.error.UserErrorCode;
import com.ioes.photo.domain.user.service.NicknameProperties;
import com.ioes.photo.domain.user.service.UserAccountService;
import com.ioes.photo.global.auth.dto.AppleLoginRequest;
import com.ioes.photo.global.auth.token.TokenResponse;
import com.ioes.photo.global.common.util.NullUtils;
import com.ioes.photo.global.auth.token.TokenService;
import com.ioes.photo.global.config.security.JwtProvider;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.AccountDeletedException;
import com.ioes.photo.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 네이티브 SDK OAuth 로그인 통합 서비스.
 *
 * 앱(iOS/Android)이 각 OAuth SDK로 직접 발급받은 토큰을 검증하고 자체 JWT를 발급합니다.
 * {@link OAuthClientRegistry}를 통해 공급자별 {@link OAuthClient}에 검증을 위임합니다.
 *
 * @author 황제연
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final OAuthClientRegistry registry;
    private final UserAccountService userAccountService;
    private final TokenService tokenService;
    private final NicknameProperties nicknameProperties;
    private final JwtProvider jwtProvider;

    /**
     * Kakao SDK 액세스 토큰으로 로그인합니다.
     *
     * @param accessToken Kakao SDK가 발급한 액세스 토큰
     */
    public TokenResponse loginWithKakao(String accessToken) {
        Map<String, String> params = Map.of("accessToken", accessToken);
        OAuthUserInfo userInfo = registry.getClient(OAuthProvider.KAKAO).getUserInfo(params);
        return processLogin(userInfo);
    }

    /**
     * Apple SDK identity token으로 로그인합니다.
     *
     * @param request Apple 로그인 요청 (identityToken 필수, user 정보는 최초 로그인 시에만 전달됨)
     */
    public TokenResponse loginWithApple(AppleLoginRequest request) {
        Map<String, String> params = new HashMap<>();
        params.put("identityToken", request.identityToken());

        String nickname = extractAppleNickname(request.user());
        if (nickname != null) {
            params.put("nickname", nickname);
        }

        OAuthUserInfo userInfo = registry.getClient(OAuthProvider.APPLE).getUserInfo(params);
        return processLogin(userInfo);
    }

    public void revokeOAuthProvider(User user) {
        OAuthClient client = registry.getClient(user.getProvider());
        String providerRefreshToken = tokenService.getProviderRefreshToken(user.getId().toString());
        client.revokeConnection(user.getProviderUserId(), providerRefreshToken);
        log.info("OAuth 연동 해제 완료: userId={}, provider={}", user.getId(), user.getProvider());
    }

    public OAuthProvider resolveProvider(String provider) {
        try {
            return OAuthProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE,
                "지원하지 않는 OAuth 공급자입니다: " + provider);
        }
    }

    private String extractAppleNickname(AppleLoginRequest.AppleUser user) {
        if (user == null || user.name() == null) {
            return null;
        }
        String firstName = NullUtils.orDefault(user.name().firstName(), "");
        String lastName  = NullUtils.orDefault(user.name().lastName(), "");
        String nickname  = (firstName + " " + lastName).trim();
        return nickname.isEmpty() ? null : nickname;
    }

    private TokenResponse processLogin(OAuthUserInfo userInfo) {
        Optional<User> deletedUser = userAccountService.findDeletedUser(userInfo.provider(), userInfo.providerId());
        if (deletedUser.isPresent()) {
            String restoreToken = jwtProvider.generateRestoreToken(deletedUser.get().getId().toString());
            throw new AccountDeletedException(restoreToken);
        }

        Optional<User> existing = userAccountService.findExistingUser(userInfo.provider(), userInfo.providerId());
        User user = existing.orElseGet(() -> createUserWithRetry(userInfo));

        String userId = user.getId().toString();
        if (userInfo.providerRefreshToken() != null) {
            tokenService.storeProviderRefreshToken(userId, userInfo.providerRefreshToken());
        }
        String[] tokens = tokenService.issueTokens(userId, user.getRole());

        TokenResponse.UserProfile profile = new TokenResponse.UserProfile(
            userId,
            user.getEmail(),
            user.getDisplayName(),
            user.getProfileImageUrl(),
            user.getProvider()
        );
        return new TokenResponse(tokens[0], tokens[1], profile);
    }

    private User createUserWithRetry(OAuthUserInfo info) {
        int maxAttempts = nicknameProperties.getHashtag().getMaxAttempts();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return userAccountService.createUser(info);
            } catch (DataIntegrityViolationException e) {
                if (attempt < maxAttempts) {
                    log.warn("닉네임 동시성 충돌, 재시도 {}/{}", attempt, maxAttempts);
                } else {
                    log.error("닉네임/해시태그 충돌로 사용자 생성 {}회 실패: provider={}, providerId={}",
                        maxAttempts, info.provider(), info.providerId());
                    throw new BusinessException(UserErrorCode.NICKNAME_GENERATION_FAILED);
                }
            }
        }
        throw new BusinessException(UserErrorCode.NICKNAME_GENERATION_FAILED);
    }
}
