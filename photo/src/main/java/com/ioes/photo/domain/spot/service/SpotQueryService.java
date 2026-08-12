package com.ioes.photo.domain.spot.service;

import com.ioes.photo.domain.spot.dto.SpotDetailResponse;
import com.ioes.photo.domain.spot.dto.SpotDetailResponse.RejectionInfo;
import com.ioes.photo.domain.spot.dto.SpotDetailResponse.SpotDetailFlags;
import com.ioes.photo.domain.spot.dto.SpotListResponse;
import com.ioes.photo.domain.spot.dto.SpotListResponse.SpotItem;
import com.ioes.photo.domain.spot.dto.SpotPreviewResponse;
import com.ioes.photo.domain.spot.dto.SpotViewportResponse;
import com.ioes.photo.domain.spot.dto.SpotViewportResponse.SpotSummary;
import com.ioes.photo.domain.spot.dto.ViewportRequest;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.enums.ReviewDecision;
import com.ioes.photo.domain.spot.enums.SortType;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.error.SpotErrorCode;
import com.ioes.photo.domain.spot.mapper.SpotMapper;
import com.ioes.photo.domain.spot.mapper.SpotPreviewRow;
import com.ioes.photo.domain.spot.mapper.SpotRow;
import com.ioes.photo.domain.spot.mapper.SpotViewportRow;
import com.ioes.photo.domain.savedspot.repository.SavedSpotArchiveRepository;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spot.repository.SpotReviewRepository;
import com.ioes.photo.domain.spotlike.repository.SpotLikeRepository;
import com.ioes.photo.domain.spotinfo.entity.SpotInfo;
import com.ioes.photo.domain.spotinfo.repository.SpotInfoRepository;
import com.ioes.photo.domain.user.repository.UserRepository;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;

import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 스팟 조회 서비스.
 *
 * @author 황제연
 */
@Service
@RequiredArgsConstructor
public class SpotQueryService {

    private static final int LIST_PAGE_SIZE = 6;

    private final SpotRepository spotRepository;
    private final SpotImageRepository spotImageRepository;
    private final SpotInfoRepository spotInfoRepository;
    private final SavedSpotArchiveRepository savedSpotArchiveRepository;
    private final SpotLikeRepository spotLikeRepository;
    private final SpotReviewRepository spotReviewRepository;
    private final SpotThumbnailService spotThumbnailService;
    private final SpotMapper spotMapper;
    private final UserRepository userRepository;

    // 비공개 스팟에 403 이 아니라 404 를 주는 이유는, 403 이 그 ID 의 스팟 존재 자체를 알려주기 때문이다.
    @Transactional
    public SpotDetailResponse findSpotDetail(Long spotId, Long userId) {
        Spot spot = spotRepository.findById(spotId)
            .orElseThrow(() -> new BusinessException(SpotErrorCode.SPOT_NOT_FOUND));

        boolean owner = spot.isOwnedBy(userId);
        if (!spot.isPublished() && !owner) {
            throw new BusinessException(SpotErrorCode.SPOT_NOT_FOUND);
        }

        // 소유자의 자가 조회는 집계에서 제외한다.
        if (spot.isPublished() && !owner) {
            spotRepository.incrementViewCount(spotId);
        }

        SpotImage spotImage = spotImageRepository.findById(spotId).orElse(null);
        SpotInfo spotInfo = spotInfoRepository.findById(spotId).orElse(null);

        String imageUrl = Optional.ofNullable(spotImage)
            .map(spotThumbnailService::getImageUrl)
            .orElse(null);

        boolean isBookmarked = userId != null
            && savedSpotArchiveRepository.findByUserIdAndSpotId(userId, spotId).isPresent();
        boolean isLiked = userId != null
            && spotLikeRepository.findByUserIdAndSpotId(userId, spotId).isPresent();
        boolean isMySpot = owner && resolveActiveUploaderId(spot.getUserId()) != null;

        SpotDetailFlags flags =
            new SpotDetailFlags(isBookmarked, isMySpot, isLiked, resolveRejection(spot, owner));

        return SpotDetailResponse.of(spot, spotImage, spotInfo, imageUrl, flags);
    }

    // 반려 사유는 운영 판단 근거라서 소유자에게만 노출한다.
    private RejectionInfo resolveRejection(Spot spot, boolean owner) {
        if (!owner || spot.getStatus() != SpotStatus.REJECTED) {
            return null;
        }
        return spotReviewRepository
            .findFirstBySpotIdAndDecisionOrderByCreatedAtDesc(spot.getId(), ReviewDecision.REJECTED)
            .map(RejectionInfo::from)
            .orElse(null);
    }

    public SpotViewportResponse findSpotsInViewport(ViewportRequest request, SpotTheme theme, Long userId) {
        String themeCode = theme != null ? theme.getCode() : null;
        List<SpotViewportRow> rows = spotMapper.findSpotsInViewport(
            request.minLat(), request.maxLat(),
            request.minLng(), request.maxLng(),
            SpotStatus.PUBLISHED.getCode(),
            themeCode
        );

        Map<Long, SpotImage> imageMap = loadImageMap(rows.stream().map(SpotViewportRow::id).toList());
        Set<Long> activeUploaderIds = resolveActiveUploaderIds(
            rows.stream().map(SpotViewportRow::userId).collect(Collectors.toSet()));

        List<SpotSummary> summaries = rows.stream()
            .map(row -> toSpotSummary(row, imageMap, userId, activeUploaderIds))
            .toList();

        return new SpotViewportResponse(summaries);
    }

    public SpotPreviewResponse findSpotPreview(Long spotId, Double latitude, Double longitude, Long userId) {
        SpotPreviewRow row = spotMapper.findSpotPreview(spotId, latitude, longitude);
        if (row == null) {
            throw new BusinessException(SpotErrorCode.SPOT_NOT_FOUND);
        }

        boolean owner = userId != null && userId.equals(row.userId());
        boolean published = SpotStatus.PUBLISHED.getCode().equals(row.status());
        if (!published && !owner) {
            throw new BusinessException(SpotErrorCode.SPOT_NOT_FOUND);
        }

        boolean isCurated = row.userId() == null;
        boolean isMySpot = owner && resolveActiveUploaderId(row.userId()) != null;
        boolean isBookmarked = userId != null
            && savedSpotArchiveRepository.findByUserIdAndSpotId(userId, spotId).isPresent();
        boolean isLiked = userId != null
            && spotLikeRepository.findByUserIdAndSpotId(userId, spotId).isPresent();
        String imageUrl = spotImageRepository.findById(spotId)
            .map(spotThumbnailService::getImageUrl)
            .orElse(null);

        return new SpotPreviewResponse(
            row.id(), row.name(), isMySpot, SpotTheme.fromCode(row.theme()),
            row.bookmarkCount(), row.distanceKm(), imageUrl,
            row.addressSimple(), row.addressRoad(), row.addressJibun(), isBookmarked,
            row.likeCount(), isLiked, published || isCurated, isCurated
        );
    }

    public SpotListResponse findSpots(int page, SpotTheme theme, Double latitude, Double longitude, SortType sort, Long userId) {
        if ((latitude == null) != (longitude == null)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "위도와 경도는 함께 입력해야 합니다.");
        }
        if (sort == SortType.DISTANCE && latitude == null) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "거리순 정렬 시 위도와 경도는 필수입니다.");
        }

        String status = SpotStatus.PUBLISHED.getCode();
        String themeCode = theme != null ? theme.getCode() : null;
        String sortCode = sort != null ? sort.getCode() : SortType.RECOMMENDED.getCode();

        List<SpotRow> rows = spotMapper.findSpots(status, themeCode, latitude, longitude, page * LIST_PAGE_SIZE, LIST_PAGE_SIZE, sortCode);
        Map<Long, SpotImage> imageMap = loadImageMap(rows.stream().map(SpotRow::id).toList());

        List<Long> spotIds = rows.stream().map(SpotRow::id).toList();
        Set<Long> bookmarkedIds = resolveBookmarkedIds(userId, spotIds);
        Set<Long> likedIds = resolveLikedIds(userId, spotIds);

        List<SpotItem> items = rows.stream()
            .map(row -> toSpotItem(row, imageMap,
                bookmarkedIds.contains(row.id()), likedIds.contains(row.id())))
            .toList();

        return new SpotListResponse(items, page, spotMapper.countSpots(status, themeCode) > (long) (page + 1) * LIST_PAGE_SIZE);
    }

    private Map<Long, SpotImage> loadImageMap(List<Long> spotIds) {
        if (spotIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return spotImageRepository.findAllBySpotIdIn(spotIds)
            .stream().collect(Collectors.toMap(SpotImage::getSpotId, image -> image));
    }

    private SpotSummary toSpotSummary(SpotViewportRow row, Map<Long, SpotImage> imageMap,
                                      Long userId, Set<Long> activeUploaderIds) {
        String thumbnailUrl = thumbnailUrl(imageMap.get(row.id()));
        boolean isMySpot = userId != null
                && activeUploaderIds.contains(row.userId()) && userId.equals(row.userId());
        return new SpotSummary(row.id(), thumbnailUrl, row.latitude(), row.longitude(), isMySpot);
    }

    private Long resolveActiveUploaderId(Long uploaderUserId) {
        if (uploaderUserId == null) {
            return null;
        }
        Set<Long> activeIds = userRepository.findActiveIdsByIdIn(Set.of(uploaderUserId));
        return activeIds.contains(uploaderUserId)
                ? uploaderUserId
                : null;
    }

    private Set<Long> resolveActiveUploaderIds(Set<Long> uploaderUserIds) {
        if (uploaderUserIds == null || uploaderUserIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> nonNull = uploaderUserIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (nonNull.isEmpty()) {
            return Collections.emptySet();
        }
        return userRepository.findActiveIdsByIdIn(nonNull);
    }

    private Set<Long> resolveBookmarkedIds(Long userId, List<Long> spotIds) {
        if (userId == null || spotIds.isEmpty()) {
            return Collections.emptySet();
        }
        return savedSpotArchiveRepository.findBookmarkedSpotIds(userId, spotIds);
    }

    private Set<Long> resolveLikedIds(Long userId, List<Long> spotIds) {
        if (userId == null || spotIds.isEmpty()) {
            return Collections.emptySet();
        }
        return spotLikeRepository.findLikedSpotIds(userId, spotIds);
    }

    private SpotItem toSpotItem(SpotRow row, Map<Long, SpotImage> imageMap,
                                boolean isBookmarked, boolean isLiked) {
        String thumbnailUrl = thumbnailUrl(imageMap.get(row.id()));
        return new SpotItem(row.id(), row.name(), row.theme(), thumbnailUrl, row.distanceKm(),
            row.bookmarkCount(), isBookmarked, row.likeCount(), isLiked);
    }

    private String thumbnailUrl(SpotImage spotImage) {
        return Optional.ofNullable(spotImage)
            .map(spotThumbnailService::getThumbnailUrl)
            .orElse(null);
    }
}
