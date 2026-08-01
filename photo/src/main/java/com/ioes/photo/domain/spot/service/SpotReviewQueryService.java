package com.ioes.photo.domain.spot.service;

import com.ioes.photo.domain.spot.dto.AdminSpotDetailResponse;
import com.ioes.photo.domain.spot.dto.AdminSpotDetailResponse.RejectionHistoryItem;
import com.ioes.photo.domain.spot.dto.AdminSpotDetailResponse.UserTrust;
import com.ioes.photo.domain.spot.dto.AdminSpotListResponse;
import com.ioes.photo.domain.spot.dto.AdminSpotListResponse.AdminSpotItem;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.entity.SpotReview;
import com.ioes.photo.domain.spot.enums.ReviewDecision;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.error.SpotErrorCode;
import com.ioes.photo.domain.spot.mapper.AdminSpotRow;
import com.ioes.photo.domain.spot.mapper.SpotReviewAdminMapper;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spot.repository.SpotReviewRepository;
import com.ioes.photo.domain.user.entity.User;
import com.ioes.photo.domain.user.repository.UserRepository;
import com.ioes.photo.global.common.util.NullUtils;
import com.ioes.photo.global.error.exception.BusinessException;
import com.ioes.photo.global.storage.StorageService;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 스팟 검수(어드민) 조회 서비스.
 *
 * 목록은 정렬/검색/상태필터/페이징을 서버에서 동적으로 처리(MyBatis)하고,
 * 상세는 유저 등록 내용 + 과거 반려 이력 + 등록 유저 신뢰도 정보를 함께 제공한다.
 *
 * @author 황제연
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpotReviewQueryService {

    private final SpotReviewAdminMapper spotReviewAdminMapper;
    private final SpotRepository spotRepository;
    private final SpotImageRepository spotImageRepository;
    private final SpotReviewRepository spotReviewRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    public AdminSpotListResponse findReviewSpots(SpotStatus status, String keyword, int page, int size) {
        String statusCode = status == null ? null : status.getCode();
        String q = NullUtils.isBlank(keyword) ? null : keyword.trim();

        List<AdminSpotRow> rows = spotReviewAdminMapper.findReviewSpots(statusCode, q, page * size, size);
        List<AdminSpotItem> items = rows.stream().map(AdminSpotItem::from).toList();

        boolean hasNext = spotReviewAdminMapper.countReviewSpots(statusCode, q) > (long) (page + 1) * size;
        return new AdminSpotListResponse(items, page, hasNext);
    }

    public AdminSpotDetailResponse getReviewSpotDetail(Long spotId) {
        Spot spot = spotRepository.findById(spotId)
            .orElseThrow(() -> new BusinessException(SpotErrorCode.SPOT_NOT_FOUND));

        User owner = userRepository.findById(spot.getUserId()).orElse(null);
        SpotImage image = spotImageRepository.findById(spotId).orElse(null);

        return new AdminSpotDetailResponse(
            spot.getId(),
            spot.getName(),
            owner == null ? null : owner.getNickname(),
            spot.getStatus().name(),
            spot.getAppliedAt(),
            resolvePhotoUrls(image),
            spot.getAddress(),
            spot.getComment(),
            resolveShotAt(image),
            spot.getTheme().name(),
            spot.getTheme().getLabel(),
            resolveRejectionHistory(spotId),
            resolveUserTrust(spot.getUserId(), owner)
        );
    }

    private List<String> resolvePhotoUrls(SpotImage image) {
        if (image == null || NullUtils.isBlank(image.getImageKey())) {
            return List.of();
        }
        String url = storageService.getUrl(image.getImageKey());
        return NullUtils.isBlank(url) ? List.of() : List.of(url);
    }

    private LocalDateTime resolveShotAt(SpotImage image) {
        if (image == null || image.getRecordedDate() == null) {
            return null;
        }
        LocalTime time = image.getRecordedTime() == null ? LocalTime.MIDNIGHT : image.getRecordedTime();
        return LocalDateTime.of(image.getRecordedDate(), time);
    }

    private List<RejectionHistoryItem> resolveRejectionHistory(Long spotId) {
        List<SpotReview> rejections = spotReviewRepository
            .findBySpotIdAndDecisionOrderByCreatedAtDesc(spotId, ReviewDecision.REJECTED);
        Map<Long, String> handlerNames = loadNicknames(
            rejections.stream().map(SpotReview::getReviewerId).toList());

        return rejections.stream()
            .map(review -> new RejectionHistoryItem(
                review.getReason() == null ? null : review.getReason().name(),
                review.getReason() == null ? null : review.getReason().getLabel(),
                review.getDetail(),
                handlerNames.get(review.getReviewerId()),
                review.getCreatedAt()
            ))
            .toList();
    }

    private UserTrust resolveUserTrust(Long userId, User owner) {
        return new UserTrust(
            owner == null ? null : owner.getCreatedAt(),
            spotRepository.countByUserId(userId),
            spotRepository.countByUserIdAndStatus(userId, SpotStatus.PUBLISHED),
            spotRepository.countByUserIdAndStatus(userId, SpotStatus.REJECTED)
        );
    }

    private Map<Long, String> loadNicknames(List<Long> userIds) {
        List<Long> distinctIds = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(distinctIds).stream()
            .collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));
    }
}
