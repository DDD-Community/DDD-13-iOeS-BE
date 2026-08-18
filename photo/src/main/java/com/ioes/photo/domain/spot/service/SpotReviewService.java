package com.ioes.photo.domain.spot.service;

import com.ioes.photo.domain.spot.dto.SpotReviewRequest;
import com.ioes.photo.domain.spot.dto.SpotReviewResultResponse;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotReview;
import com.ioes.photo.domain.spot.enums.RejectionReason;
import com.ioes.photo.domain.spot.enums.SpotOpenRequestStatus;
import com.ioes.photo.domain.spot.error.SpotErrorCode;
import com.ioes.photo.domain.spot.repository.SpotOpenRequestRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spot.repository.SpotReviewRepository;
import com.ioes.photo.global.common.util.NullUtils;
import com.ioes.photo.global.error.exception.BusinessException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 스팟 검수(승인/반려) 처리 서비스.
 *
 * 검수 가능한 상태(PENDING/RE_REVIEW_PENDING)에서만 처리되며, 그 외 상태는 이미 처리된 것으로 보고 409로 응답한다.
 * 사용자의 오픈 철회와 같은 행을 동시에 건드릴 수 있어, 조회 시점부터 행 잠금을 잡아 상태 판단을 직렬화한다.
 * 승인 시 미승인(PRIVATE) 이미지를 공개(PUBLIC) 경로로 옮겨 지도 공개용 영구 URL을 확보한다.
 *
 * @author 황제연
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpotReviewService {

    private final SpotRepository spotRepository;
    private final SpotReviewRepository spotReviewRepository;
    private final SpotOpenRequestRepository spotOpenRequestRepository;
    private final SpotImageAccessService spotImageAccessService;

    @Transactional
    public SpotReviewResultResponse review(Long spotId, SpotReviewRequest request, Long reviewerId) {
        Spot spot = spotRepository.findWithLockById(spotId)
            .orElseThrow(() -> new BusinessException(SpotErrorCode.SPOT_NOT_FOUND));

        if (!spot.isReviewable()) {
            throw new BusinessException(SpotErrorCode.SPOT_ALREADY_REVIEWED);
        }

        SpotReview review = request.decision().isApproved()
            ? approve(spot, reviewerId)
            : reject(spot, request, reviewerId);

        resolveOpenRequest(spotId, request.decision().isApproved(), review.getId(), spot.getReviewedAt());

        return new SpotReviewResultResponse(spot.getId(), spot.getStatus().name());
    }

    // saveAndFlush 로 식별자를 확보한 뒤, 오픈 신청 이력이 이 검수 건을 가리키도록 연결한다.
    private SpotReview approve(Spot spot, Long reviewerId) {
        spot.applyReview(true, reviewerId, LocalDateTime.now());
        SpotReview review = SpotReview.approved(spot.getId(), reviewerId);
        spotReviewRepository.saveAndFlush(review);
        spotImageAccessService.publish(spot.getId());
        return review;
    }

    private SpotReview reject(Spot spot, SpotReviewRequest request, Long reviewerId) {
        RejectionReason reason = request.reason();
        if (reason == null) {
            throw new BusinessException(SpotErrorCode.SPOT_REJECTION_REASON_REQUIRED);
        }
        if (reason.requiresDetail() && NullUtils.isBlank(request.detail())) {
            throw new BusinessException(SpotErrorCode.SPOT_REJECTION_DETAIL_REQUIRED);
        }

        spot.applyReview(false, reviewerId, LocalDateTime.now());
        SpotReview review = SpotReview.rejected(spot.getId(), reviewerId, reason, request.detail());
        spotReviewRepository.saveAndFlush(review);
        return review;
    }

    // 검수 도입 이전에 신청된 스팟은 대응하는 이력이 없으므로 마감 대상에서 조용히 빠진다.
    private void resolveOpenRequest(Long spotId, boolean approved, Long reviewId, LocalDateTime reviewedAt) {
        spotOpenRequestRepository
            .findFirstBySpotIdAndStatusOrderByRequestedAtDesc(spotId, SpotOpenRequestStatus.REQUESTED)
            .ifPresent(openRequest -> openRequest.resolveByReview(approved, reviewId, reviewedAt));
    }
}
