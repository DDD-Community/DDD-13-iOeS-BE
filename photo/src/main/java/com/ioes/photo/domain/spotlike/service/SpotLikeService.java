package com.ioes.photo.domain.spotlike.service;

import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.error.SpotErrorCode;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spotlike.dto.SpotLikeResponse;
import com.ioes.photo.domain.spotlike.entity.SpotLike;
import com.ioes.photo.domain.spotlike.error.SpotLikeErrorCode;
import com.ioes.photo.domain.spotlike.repository.SpotLikeRepository;
import com.ioes.photo.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 스팟 좋아요(추천) 서비스.
 *
 * 동시성: (user_id, spot_id) DB UNIQUE 제약 + saveAndFlush 로 중복 좋아요를 막고,
 * likeCount 는 atomic JPQL update 로 갱신해 Lost Update 를 피한다.
 * 여러 사용자가 동시에 누르더라도 응답은 항상 재조회한 최종 값이므로,
 * 클라이언트는 그 값으로 화면을 다시 맞추면 된다.
 *
 * 좋아요는 승인(PUBLISHED)된 스팟에만 허용한다.
 * 다만 관리자 큐레이션(user_id IS NULL) 스팟은 검수 절차를 거치지 않으므로 상태와 무관하게 허용한다.
 *
 * @author 황제연
 */
@Service
@RequiredArgsConstructor
public class SpotLikeService {

    private final SpotLikeRepository spotLikeRepository;
    private final SpotRepository spotRepository;

    @Transactional
    public SpotLikeResponse addLike(Long userId, Long spotId) {
        validateLikeable(spotId);

        spotLikeRepository.findByUserIdAndSpotIdIncludingDeleted(userId, spotId)
            .ifPresentOrElse(
                like -> {
                    if (like.isActive()) {
                        throw new BusinessException(SpotLikeErrorCode.ALREADY_LIKED);
                    }
                    like.restore();
                },
                () -> insertLike(userId, spotId)
            );

        spotRepository.incrementLikeCount(spotId);
        return new SpotLikeResponse(fetchLikeCount(spotId), true);
    }

    @Transactional
    public SpotLikeResponse removeLike(Long userId, Long spotId) {
        validateSpotExists(spotId);

        SpotLike like = spotLikeRepository.findByUserIdAndSpotId(userId, spotId)
            .orElseThrow(() -> new BusinessException(SpotLikeErrorCode.NOT_LIKED));

        like.softDelete();
        spotRepository.decrementLikeCount(spotId);
        return new SpotLikeResponse(fetchLikeCount(spotId), false);
    }

    private void validateLikeable(Long spotId) {
        Spot spot = findSpotIncludingDeleted(spotId);
        if (spot.isDeleted()) {
            throw new BusinessException(SpotErrorCode.SPOT_DELETED);
        }
        if (!spot.isLikeable()) {
            throw new BusinessException(SpotLikeErrorCode.SPOT_NOT_LIKEABLE);
        }
    }

    private void validateSpotExists(Long spotId) {
        findSpotIncludingDeleted(spotId);
    }

    private Spot findSpotIncludingDeleted(Long spotId) {
        return spotRepository.findByIdIncludingDeleted(spotId)
            .orElseThrow(() -> new BusinessException(SpotErrorCode.SPOT_NOT_FOUND));
    }

    private void insertLike(Long userId, Long spotId) {
        try {
            spotLikeRepository.saveAndFlush(
                SpotLike.builder().userId(userId).spotId(spotId).build()
            );
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(SpotLikeErrorCode.ALREADY_LIKED);
        }
    }

    private long fetchLikeCount(Long spotId) {
        return spotRepository.findLikeCountById(spotId).orElse(0L);
    }
}
