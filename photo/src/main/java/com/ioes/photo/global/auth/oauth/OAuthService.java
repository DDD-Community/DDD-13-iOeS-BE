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
import java.util.Optional;

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

    public String getAuthorizationUrl(OAuthProvider provider) {
        return registry.getClient(provider).getAuthorizationUrl();
    }

    @Transactional
    public TokenResponse handleCallback(OAuthProvider provider, Map<String, String> params) {
        OAuthUserInfo userInfo = registry.getClient(provider).getUserInfo(params);
        return processLogin(userInfo);
    }

    public OAuthProvider resolveProvider(String provider) {
        try {
            return OAuthProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE,
                "지원하지 않는 OAuth 공급자입니다: " + provider);
        }
    }

    private TokenResponse processLogin(OAuthUserInfo userInfo) {
        Optional<User> existing = userRepository
            .findByProviderAndProviderUserId(userInfo.provider(), userInfo.providerId());

        User user = existing.map(value -> updateProfile(value, userInfo))
                .orElseGet(() -> createUser(userInfo));

        String userId = user.getId().toString();
        String[] tokens = tokenService.issueTokens(userId);

        TokenResponse.UserProfile profile = new TokenResponse.UserProfile(
            userId,
            user.getEmail(),
            user.getNickname(),
            user.getProfileImageUrl(),
            user.getProvider()
        );
        return new TokenResponse(tokens[0], tokens[1], profile);
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