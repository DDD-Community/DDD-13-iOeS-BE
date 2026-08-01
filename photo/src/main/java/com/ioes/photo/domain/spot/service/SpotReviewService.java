package com.ioes.photo.domain.spot.service;

import com.ioes.photo.domain.spot.dto.SpotReviewRequest;
import com.ioes.photo.domain.spot.dto.SpotReviewResultResponse;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.entity.SpotReview;
import com.ioes.photo.domain.spot.enums.RejectionReason;
import com.ioes.photo.domain.spot.error.SpotErrorCode;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spot.repository.SpotReviewRepository;
import com.ioes.photo.global.common.util.NullUtils;
import com.ioes.photo.global.error.exception.BusinessException;
import com.ioes.photo.global.storage.AccessType;
import com.ioes.photo.global.storage.StorageCleanupEvent;
import com.ioes.photo.global.storage.StoragePathUtils;
import com.ioes.photo.global.storage.StorageService;
import com.ioes.photo.global.storage.StorageUploadRollbackEvent;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 스팟 검수(승인/반려) 처리 서비스.
 *
 * 검수 가능한 상태(PENDING/RE_REVIEW_PENDING)에서만 처리되며, 그 외 상태는 이미 처리된 것으로 보고 409로 응답한다.
 * 승인 시 미승인(PRIVATE) 이미지를 공개(PUBLIC) 경로로 이동해 지도 공개용 영구 URL을 확보한다.
 * 이미지 이동은 복사만 트랜잭션 안에서 수행하고, 원본 삭제는 커밋 이후(StorageCleanupEvent),
 * 사본 정리는 롤백 이후(StorageUploadRollbackEvent)로 미뤄 DB와 스토리지의 정합성을 맞춘다.
 *
 * @author 황제연
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpotReviewService {

    private final SpotRepository spotRepository;
    private final SpotReviewRepository spotReviewRepository;
    private final SpotImageRepository spotImageRepository;
    private final StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public SpotReviewResultResponse review(Long spotId, SpotReviewRequest request, Long reviewerId) {
        Spot spot = spotRepository.findById(spotId)
            .orElseThrow(() -> new BusinessException(SpotErrorCode.SPOT_NOT_FOUND));

        // 검수 동시성 미방어(check-then-act). 운영자 2~3명 규모라 같은 건을 동시에 처리할
        // 확률이 희박하고, 겹치더라도 승인+승인이면 상태가 동일해 피해가 검수 이력 중복에 그친다.
        // 운영자가 늘거나 검수가 자동화되면 findWithLockById(PESSIMISTIC_WRITE)로 전환한다.
        if (!spot.isReviewable()) {
            throw new BusinessException(SpotErrorCode.SPOT_ALREADY_REVIEWED);
        }

        if (request.decision().isApproved()) {
            approve(spot, reviewerId);
        } else {
            reject(spot, request, reviewerId);
        }

        return new SpotReviewResultResponse(spot.getId(), spot.getStatus().name());
    }

    private void approve(Spot spot, Long reviewerId) {
        spot.applyReview(true, reviewerId, LocalDateTime.now());
        spotReviewRepository.save(SpotReview.approved(spot.getId(), reviewerId));
        publishImages(spot.getId());
    }

    private void reject(Spot spot, SpotReviewRequest request, Long reviewerId) {
        RejectionReason reason = request.reason();
        if (reason == null) {
            throw new BusinessException(SpotErrorCode.SPOT_REJECTION_REASON_REQUIRED);
        }
        if (reason.requiresDetail() && NullUtils.isBlank(request.detail())) {
            throw new BusinessException(SpotErrorCode.SPOT_REJECTION_DETAIL_REQUIRED);
        }

        spot.applyReview(false, reviewerId, LocalDateTime.now());
        spotReviewRepository.save(SpotReview.rejected(spot.getId(), reviewerId, reason, request.detail()));
    }

    private void publishImages(Long spotId) {
        spotImageRepository.findById(spotId).ifPresent(image -> {
            image.updateImageKey(moveToPublic(image.getImageKey()));
            if (NullUtils.isNotBlank(image.getThumbnailKey())) {
                image.updateThumbnailKey(moveToPublic(image.getThumbnailKey()));
            }
        });
    }

    private String moveToPublic(String key) {
        if (NullUtils.isBlank(key) || StoragePathUtils.isPublic(key)) {
            return key;
        }
        String publicKey = StoragePathUtils.withAccess(key, AccessType.PUBLIC);
        storageService.copy(key, publicKey);
        eventPublisher.publishEvent(new StorageUploadRollbackEvent(publicKey));
        eventPublisher.publishEvent(new StorageCleanupEvent(key));
        return publicKey;
    }
}
