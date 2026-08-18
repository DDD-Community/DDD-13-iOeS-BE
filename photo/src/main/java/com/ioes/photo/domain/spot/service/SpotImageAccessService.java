package com.ioes.photo.domain.spot.service;

import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.global.common.util.NullUtils;
import com.ioes.photo.global.storage.AccessType;
import com.ioes.photo.global.storage.StorageCleanupEvent;
import com.ioes.photo.global.storage.StoragePathUtils;
import com.ioes.photo.global.storage.StorageService;
import com.ioes.photo.global.storage.StorageUploadRollbackEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 스팟 이미지의 공개 범위(PUBLIC/PRIVATE) 전환 서비스.
 *
 * "스팟이 공개(PUBLISHED)면 이미지도 PUBLIC 경로에 있다"는 불변식을 유지하는 단일 지점이다.
 * 승인 시 공개로 올리고, 사용자가 비공개로 되돌리면 다시 내린다.
 * PUBLIC 키는 만료 없는 CloudFront URL로 서빙되므로, 내려주지 않으면 URL을 아는 누구나
 * 비공개 전환 이후에도 사진에 접근할 수 있다.
 *
 * 복사만 트랜잭션 안에서 수행하고 원본 삭제는 커밋 이후(StorageCleanupEvent),
 * 사본 정리는 롤백 이후(StorageUploadRollbackEvent)로 미뤄 DB와 스토리지의 정합성을 맞춘다.
 *
 * @author 황제연
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpotImageAccessService {

    private final SpotImageRepository spotImageRepository;
    private final StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void publish(Long spotId) {
        moveImages(spotId, AccessType.PUBLIC);
    }

    @Transactional
    public void unpublish(Long spotId) {
        moveImages(spotId, AccessType.PRIVATE);
    }

    private void moveImages(Long spotId, AccessType target) {
        spotImageRepository.findById(spotId).ifPresent(image -> {
            // 외부 호스팅 이미지는 PUBLIC/PRIVATE 경로 개념이 없어 이동하지 않는다.
            if (image.isExternal()) {
                return;
            }
            image.updateImageKey(move(image.getImageKey(), target));
            if (NullUtils.isNotBlank(image.getThumbnailKey())) {
                image.updateThumbnailKey(move(image.getThumbnailKey(), target));
            }
        });
    }

    private String move(String key, AccessType target) {
        if (NullUtils.isBlank(key) || isAlreadyAt(key, target)) {
            return key;
        }

        String movedKey = StoragePathUtils.withAccess(key, target);
        if (movedKey.equals(key)) {
            log.warn("스팟 이미지 경로에 접근 구분이 없어 이동을 건너뜁니다. key={}", key);
            return key;
        }

        storageService.copy(key, movedKey);
        eventPublisher.publishEvent(new StorageUploadRollbackEvent(movedKey));
        eventPublisher.publishEvent(new StorageCleanupEvent(key));
        return movedKey;
    }

    private boolean isAlreadyAt(String key, AccessType target) {
        return StoragePathUtils.isPublic(key) == (target == AccessType.PUBLIC);
    }
}
