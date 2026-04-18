package com.ioes.photo.domain.user.service;

import com.ioes.photo.domain.user.dto.UpdateProfileRequest;
import com.ioes.photo.domain.user.dto.UpdateProfileResponse;
import com.ioes.photo.domain.user.entity.User;
import com.ioes.photo.domain.user.error.UserErrorCode;
import com.ioes.photo.domain.user.repository.UserRepository;
import com.ioes.photo.global.auth.oauth.OAuthService;
import com.ioes.photo.global.auth.token.TokenService;
import com.ioes.photo.global.common.util.NullUtils;
import com.ioes.photo.global.error.exception.BusinessException;
import com.ioes.photo.global.storage.StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 사용자 도메인 서비스
 *
 * @author 황제연
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final StorageService storageService;
    private final OAuthService oAuthService;

    @Transactional
    public UpdateProfileResponse updateProfile(Long userId, UpdateProfileRequest request, MultipartFile profileImage) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        updateNicknameIfPresent(user, request.nickname());
        updateEmailIfPresent(user, request.email());
        updateProfileImageIfPresent(user, profileImage);

        return UpdateProfileResponse.from(user);
    }

    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        try {
            oAuthService.revokeOAuthProvider(user);
        } catch (Exception e) {
            log.warn("OAuth 연동 해제 실패 (탈퇴 계속 진행): userId={}, provider={}", userId, user.getProvider(), e);
        }

        tokenService.invalidateAllUserTokens(userId.toString());
        userRepository.softDeleteById(userId);
        log.info("회원탈퇴 완료: userId={}", userId);
    }

    private void updateNicknameIfPresent(User user, String nickname) {
        if (NullUtils.isBlank(nickname)) {
            return;
        }
        if (nickname.equals(user.getNickname())) {
            return;
        }

        // 업데이트 시, 닉네임 중복 체크 여부는 정책 논의해야함.
        user.updateProfile(null, nickname, null);
    }

    private void updateEmailIfPresent(User user, String email) {
        if (NullUtils.isBlank(email)) {
            return;
        }
        if (NullUtils.isBlank(user.getEmail())) {
            throw new BusinessException(UserErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        user.updateProfile(email, null, null);
    }

    private void updateProfileImageIfPresent(User user, MultipartFile profileImage) {
        if (profileImage == null || profileImage.isEmpty()) {
            return;
        }

        String imageUrl = storageService.uploadImage(profileImage);
        user.updateProfile(null, null, imageUrl);
    }
}
