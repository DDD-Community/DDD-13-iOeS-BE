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
import com.ioes.photo.global.storage.AccessType;
import com.ioes.photo.global.storage.StorageCleanupEvent;
import com.ioes.photo.global.storage.StoragePathUtils;
import com.ioes.photo.global.storage.StorageService;
import com.ioes.photo.global.storage.UploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 사용자 도메인 서비스.
 *
 * 프로필 이미지 S3 키 전략
 * - 사용자가 직접 업로드한 프로필 이미지는 S3에 구조화된 경로로 저장되며, DB에는 URL이 아닌 S3 키만 보관합니다.
 * 응답 시점에 URL을 동적 생성합니다.
 *
 * 경로: {env}/public/users/{userId}/profile/{yyyyMM}/{uuid}.{ext}
 * - PUBLIC 경로이므로 CloudFront URL 제공 (만료 없음)
 * - 이미지 교체 시 기존 S3 파일 삭제 + CloudFront 무효화
 *
 *
 * @author 황제연
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private static final String ENTITY = "users";
    private static final String TYPE_PROFILE = "profile";

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final StorageService storageService;
    private final OAuthService oAuthService;
    private final Environment environment;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UpdateProfileResponse updateProfile(Long userId, UpdateProfileRequest request,
                                               MultipartFile profileImage) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        updateNicknameIfPresent(user, request.nickname());
        updateEmailIfPresent(user, request.email());
        updateProfileImageIfPresent(user, profileImage);

        return UpdateProfileResponse.from(user, resolveProfileImageUrl(user));
    }

    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        // 사용자가 직접 업로드한 프로필 이미지 S3 삭제 + CloudFront 무효화 (커밋 후 실행)
        if (user.getProfileImageKey() != null) {
            eventPublisher.publishEvent(new StorageCleanupEvent(user.getProfileImageKey()));
        }

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
        if (NullUtils.isBlank(nickname) || nickname.equals(user.getNickname())) {
            return;
        }
        user.updateProfile(null, nickname, null);
    }

    private void updateEmailIfPresent(User user, String email) {
        if (NullUtils.isBlank(email)) {
            return;
        }
        if (NullUtils.isNotBlank(user.getEmail())) {
            throw new BusinessException(UserErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        user.updateProfile(email, null, null);
    }

    private void updateProfileImageIfPresent(User user, MultipartFile profileImage) {
        if (profileImage == null || profileImage.isEmpty()) {
            return;
        }

        String newKey = StoragePathUtils.generate(
            storageProperties.env(), AccessType.PUBLIC, ENTITY, user.getId(), TYPE_PROFILE,
            profileImage.getOriginalFilename());

        String oldKey = user.getProfileImageKey();
        UploadResult result = storageService.upload(profileImage, newKey);

        user.updateProfileImageKey(result.key());

        // 기존 업로드 이미지 정리 — DB 커밋 성공 후 실행
        if (NullUtils.isNotBlank(oldKey)) {
            eventPublisher.publishEvent(new StorageCleanupEvent(oldKey));
        }
    }

    private String resolveProfileImageUrl(User user) {
        if (NullUtils.isNotBlank(user.getProfileImageKey())){
            return storageService.getUrl(user.getProfileImageKey());
        }
        return user.getProfileImageUrl();
    }

}