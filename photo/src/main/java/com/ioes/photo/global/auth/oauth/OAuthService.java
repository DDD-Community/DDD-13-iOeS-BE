package com.ioes.photo.global.auth.oauth;

import com.ioes.photo.domain.user.entity.User;
import com.ioes.photo.domain.user.repository.UserRepository;
import com.ioes.photo.domain.user.service.NicknameGenerator;
import com.ioes.photo.global.auth.token.TokenResponse;
import com.ioes.photo.global.auth.token.TokenService;
import com.ioes.photo.domain.user.error.UserErrorCode;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * OAuth 로그인 통합 서비스.
 *
 * {@link OAuthClientRegistry}를 통해 공급자별 {@link OAuthClient}에 처리를 위임합니다.
 * State(CSRF 방지)와 PKCE(code 가로채기 방지) 생성/검증을 담당합니다.
 * 새로운 OAuth 공급자를 추가할 때 이 클래스를 수정하지 않아도 됩니다.
 *
 * @author 황제연
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OAuthClientRegistry registry;
    private final OAuthStateStore stateStore;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final NicknameGenerator nicknameGenerator;

    public String getAuthorizationUrl(OAuthProvider provider) {
        String state = generateState();
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);

        stateStore.save(state, codeVerifier);

        return registry.getClient(provider).buildAuthorizationUrl(state, codeChallenge);
    }

    @Transactional
    public TokenResponse handleCallback(OAuthProvider provider, Map<String, String> params) {
        Map<String, String> enrichedParams = validateStateAndEnrich(params);
        OAuthUserInfo userInfo = registry.getClient(provider).getUserInfo(enrichedParams);
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

    private Map<String, String> validateStateAndEnrich(Map<String, String> params) {
        String state = params.get("state");
        if (state == null || state.isBlank()) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "state 파라미터가 누락되었습니다.");
        }

        String codeVerifier = stateStore.getAndDelete(state);
        if (codeVerifier == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED,
                "유효하지 않거나 만료된 state입니다. 처음부터 다시 로그인해주세요.");
        }

        Map<String, String> enriched = new HashMap<>(params);
        enriched.put("code_verifier", codeVerifier);
        return enriched;
    }

    private TokenResponse processLogin(OAuthUserInfo userInfo) {
        Optional<User> existing = userRepository
            .findByProviderAndProviderUserId(userInfo.provider(), userInfo.providerId());

        User user = existing.map(value -> updateProfile(value, userInfo))
                .orElseGet(() -> createUser(userInfo));

        String userId = user.getId().toString();
        if (userInfo.providerRefreshToken() != null) {
            tokenService.storeProviderRefreshToken(userId, userInfo.providerRefreshToken());
        }
        String[] tokens = tokenService.issueTokens(userId);

        TokenResponse.UserProfile profile = new TokenResponse.UserProfile(
            userId,
            user.getEmail(),
            user.getDisplayName(),
            user.getProfileImageUrl(),
            user.getProvider()
        );
        return new TokenResponse(tokens[0], tokens[1], profile);
    }

    private User createUser(OAuthUserInfo info) {
        if (info.nickname() != null) {
            return saveUser(info, info.nickname(), null);
        }

        int maxAttempts = nicknameGenerator.maxAttempts();
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            NicknameGenerator.Result generated = nicknameGenerator.generate();
            try {
                return saveUser(info, generated.nickname(), generated.hashTag());
            } catch (DataIntegrityViolationException e) {
                log.warn("닉네임 동시성 충돌, 재시도 {}/{}: nickname={}, hashTag={}",
                    attempt + 1, maxAttempts, generated.nickname(), generated.hashTag());
            }
        }

        throw new BusinessException(UserErrorCode.NICKNAME_GENERATION_FAILED);
    }

    private User saveUser(OAuthUserInfo info, String nickname, Long hashTag) {
        User user = User.builder()
            .provider(info.provider())
            .providerUserId(info.providerId())
            .email(info.email())
            .nickname(nickname)
            .profileImageUrl(info.profileImageUrl())
            .hashTag(hashTag)
            .build();
        return userRepository.save(user);
    }

    private User updateProfile(User user, OAuthUserInfo info) {
        user.updateProfile(info.email(), info.nickname(), info.profileImageUrl());
        return user;
    }
    private String generateState() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    private String generateCodeVerifier() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}