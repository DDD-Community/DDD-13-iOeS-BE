package com.ioes.photo.global.auth.oauth;

import com.ioes.photo.domain.user.entity.User;
import com.ioes.photo.domain.user.repository.UserRepository;
import com.ioes.photo.global.auth.token.TokenResponse;
import com.ioes.photo.global.auth.token.TokenService;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * OAuth 로그인 통합 서비스.
 *
 * <p>{@link OAuthClientRegistry}를 통해 공급자별 {@link OAuthClient}에 처리를 위임합니다.
 * 새로운 OAuth 공급자를 추가할 때 이 클래스를 수정하지 않아도 됩니다.
 *
 * @author 황제연
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final OAuthClientRegistry registry;
    private final UserRepository       userRepository;
    private final TokenService         tokenService;

    /**
     * OAuth 로그인 인증 URL을 반환합니다.
     *
     * @param provider OAuth 공급자
     * @return 인증 페이지 URL
     */
    public String getAuthorizationUrl(OAuthProvider provider) {
        return registry.getClient(provider).getAuthorizationUrl();
    }

    /**
     * OAuth 콜백을 처리합니다.
     *
     * <p>공급자에 해당하는 {@link OAuthClient}에 사용자 정보 조회를 위임하고,
     * 회원을 조회 또는 생성한 뒤 서버 자체 토큰을 발급합니다.
     *
     * @param provider OAuth 공급자
     * @param params   공급자가 콜백으로 전달한 파라미터 (code, user 등)
     * @return 발급된 토큰 및 프로필 정보
     */
    @Transactional
    public TokenResponse handleCallback(OAuthProvider provider, Map<String, String> params) {
        OAuthUserInfo userInfo = registry.getClient(provider).getUserInfo(params);
        return processLogin(userInfo);
    }

    /**
     * provider 문자열을 {@link OAuthProvider} 열거형으로 변환합니다.
     *
     * @param provider 공급자 이름 문자열 (대소문자 무관)
     * @throws BusinessException 지원하지 않는 공급자인 경우
     */
    public OAuthProvider resolveProvider(String provider) {
        try {
            return OAuthProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE,
                "지원하지 않는 OAuth 공급자입니다: " + provider);
        }
    }

    // ── private ──────────────────────────────────────────────────────────────

    private TokenResponse processLogin(OAuthUserInfo userInfo) {
        boolean[] isNew = {false};

        User user = userRepository
            .findByProviderAndProviderUserId(userInfo.provider(), userInfo.providerId())
            .map(existing -> updateProfile(existing, userInfo))
            .orElseGet(() -> {
                isNew[0] = true;
                return createUser(userInfo);
            });

        String userId   = user.getId().toString();
        String[] tokens = tokenService.issueTokens(userId);

        log.info("OAuth {} 완료: provider={}, userId={}",
            isNew[0] ? "회원가입" : "로그인", userInfo.provider(), userId);

        TokenResponse.UserProfile profile = new TokenResponse.UserProfile(
            userId,
            user.getEmail(),
            user.getNickname(),
            user.getProfileImageUrl(),
            user.getProvider()
        );
        return new TokenResponse(tokens[0], tokens[1], profile, isNew[0]);
    }

    private User createUser(OAuthUserInfo info) {
        User user = User.builder()
            .provider(info.provider())
            .providerUserId(info.providerId())
            .email(info.email())
            .nickname(info.nickname())
            .profileImageUrl(info.profileImageUrl())
            .build();
        return userRepository.save(user);
    }

    private User updateProfile(User user, OAuthUserInfo info) {
        user.updateProfile(info.email(), info.nickname(), info.profileImageUrl());
        return user;
    }
}