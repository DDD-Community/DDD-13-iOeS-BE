package com.ioes.photo.domain.user.service;

import com.ioes.photo.domain.user.entity.User;
import com.ioes.photo.domain.user.repository.UserRepository;
import com.ioes.photo.global.auth.oauth.OAuthProvider;
import com.ioes.photo.global.auth.oauth.OAuthUserInfo;
import com.ioes.photo.global.common.util.NullUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * OAuth 로그인 시 사용자 조회/생성/프로필 갱신을 담당하는 서비스.
 *
 * createUser는 닉네임/해시태그 동시성 충돌(DataIntegrityViolationException) 발생 시 최대 5회 재시도한다.
 *
 * @author 황제연
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserRepository userRepository;
    private final NicknameGenerator nicknameGenerator;

    @Transactional(readOnly = true)
    public Optional<User> findExistingUser(OAuthProvider provider, String providerId) {
        return userRepository.findByProviderAndProviderUserId(provider, providerId);
    }

    @Transactional
    public User createUser(OAuthUserInfo info) {
        NicknameGenerator.Result resolved = resolveNickname(info);
        return userRepository.save(User.builder()
            .provider(info.provider())
            .providerUserId(info.providerId())
            .email(info.email())
            .nickname(resolved.nickname())
            .profileImageUrl(info.profileImageUrl())
            .hashTag(resolved.hashTag())
            .build());
    }

    private NicknameGenerator.Result resolveNickname(OAuthUserInfo info) {
        if(NullUtils.isNotBlank(info.nickname())) {
            return new NicknameGenerator.Result(info.nickname(), null);
        }
        return nicknameGenerator.generate();
    }

}