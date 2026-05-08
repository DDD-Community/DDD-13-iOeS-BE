package com.ioes.photo.domain.user.service;

import com.ioes.photo.domain.user.dto.ArchiveImageResponse;
import com.ioes.photo.domain.user.entity.User;
import com.ioes.photo.domain.user.error.UserErrorCode;
import com.ioes.photo.domain.user.repository.UserRepository;
import com.ioes.photo.global.common.util.NullUtils;
import com.ioes.photo.global.config.s3.properties.StorageProperties;
import com.ioes.photo.global.error.exception.BusinessException;
import com.ioes.photo.global.storage.AccessType;
import com.ioes.photo.global.storage.StorageCleanupEvent;
import com.ioes.photo.global.storage.StoragePathUtils;
import com.ioes.photo.global.storage.StorageService;
import com.ioes.photo.global.storage.StorageUploadRollbackEvent;
import com.ioes.photo.global.storage.UploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 사용자 보관함 이미지 서비스.
 *
 * 보관함 이미지는 PRIVATE 접근으로 관리되며, 조회 시 Presigned URL을 동적으로 생성합니다.
 * S3 경로: {env}/private/users/{userId}/archive/{yyyyMM}/{uuid}.{ext}
 *
 * @author 황제연
 */
@Service
@RequiredArgsConstructor
public class UserArchiveService {

    private static final String ENTITY = "users";
    private static final String TYPE_ARCHIVE = "archive";

    private final UserRepository userRepository;
    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ArchiveImageResponse updateArchiveImage(Long userId, MultipartFile archiveImage) {
        User user = findUser(userId);
        String oldKey = user.getArchiveImageKey();

        String newKey = StoragePathUtils.generate(
            storageProperties.env(), AccessType.PRIVATE, ENTITY, user.getId(), TYPE_ARCHIVE,
            archiveImage.getOriginalFilename()
        );

        UploadResult result = storageService.upload(archiveImage, newKey);
        eventPublisher.publishEvent(new StorageUploadRollbackEvent(result.key()));

        user.updateArchiveImageKey(result.key());

        if (NullUtils.isNotBlank(oldKey)) {
            eventPublisher.publishEvent(new StorageCleanupEvent(oldKey));
        }

        return ArchiveImageResponse.of(storageService.getUrl(result.key()));
    }

    @Transactional(readOnly = true)
    public ArchiveImageResponse getArchiveImage(Long userId) {
        User user = findUser(userId);
        String url = NullUtils.isNotBlank(user.getArchiveImageKey())
                ? storageService.getUrl(user.getArchiveImageKey())
                : null;
        return ArchiveImageResponse.of(url);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }
}