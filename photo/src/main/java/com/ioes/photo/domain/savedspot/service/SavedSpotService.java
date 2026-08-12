package com.ioes.photo.domain.savedspot.service;

import com.ioes.photo.domain.savedspot.dto.BookmarkResponse;
import com.ioes.photo.domain.savedspot.dto.SavedSpotListResponse;
import com.ioes.photo.domain.savedspot.dto.SavedSpotListResponse.SavedSpotItem;
import com.ioes.photo.domain.savedspot.entity.SavedSpotArchive;
import com.ioes.photo.domain.savedspot.error.SavedSpotErrorCode;
import com.ioes.photo.domain.savedspot.mapper.SavedSpotMapper;
import com.ioes.photo.domain.savedspot.mapper.SavedSpotRow;
import com.ioes.photo.domain.savedspot.repository.SavedSpotArchiveRepository;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.error.SpotErrorCode;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.global.common.util.NullUtils;
import com.ioes.photo.global.error.exception.BusinessException;
import com.ioes.photo.global.storage.StorageService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 저장된 스팟(북마크) 서비스.
 *
 * 북마크 지정 동시성: (user_id, spot_id) DB UNIQUE constraint + saveAndFlush로 중복 방지.
 * bookmarkCount는 atomic JPQL update로 Lost Update 없이 처리한다.
 *
 * @author 황제연
 */
@Service
@RequiredArgsConstructor
public class SavedSpotService {

    private static final int PAGE_SIZE = 6;

    private final SavedSpotArchiveRepository savedSpotArchiveRepository;
    private final SpotRepository spotRepository;
    private final SpotImageRepository spotImageRepository;
    private final StorageService storageService;
    private final SavedSpotMapper savedSpotMapper;

    @Transactional
    public BookmarkResponse addBookmark(Long userId, Long spotId) {
        validateSpotBookmarkable(spotId);

        savedSpotArchiveRepository.findByUserIdAndSpotIdIncludingDeleted(userId, spotId)
            .ifPresentOrElse(
                archive -> {
                    if (archive.isActive()) {
                        throw new BusinessException(SavedSpotErrorCode.ALREADY_BOOKMARKED);
                    }
                    archive.restore();
                },
                () -> insertArchive(userId, spotId)
            );

        spotRepository.incrementBookmarkCount(spotId);
        return new BookmarkResponse(fetchBookmarkCount(spotId));
    }

    @Transactional
    public BookmarkResponse removeBookmark(Long userId, Long spotId) {
        validateSpotExists(spotId);

        SavedSpotArchive archive = savedSpotArchiveRepository.findByUserIdAndSpotId(userId, spotId)
            .orElseThrow(() -> new BusinessException(SavedSpotErrorCode.NOT_BOOKMARKED));

        archive.softDelete();
        spotRepository.decrementBookmarkCount(spotId);
        return new BookmarkResponse(fetchBookmarkCount(spotId));
    }

    public SavedSpotListResponse findSavedSpots(Long userId, int page, Double latitude, Double longitude) {
        List<SavedSpotRow> rows = savedSpotMapper.findSavedSpots(
            userId, latitude, longitude, page * PAGE_SIZE, PAGE_SIZE
        );

        Map<Long, SpotImage> imageMap = loadImageMap(rows.stream().map(SavedSpotRow::spotId).toList());

        List<SavedSpotItem> items = rows.stream()
            .map(row -> toSavedSpotItem(row, imageMap))
            .toList();

        boolean hasNext = savedSpotMapper.countSavedSpots(userId) > (long) (page + 1) * PAGE_SIZE;
        return new SavedSpotListResponse(items, page, hasNext);
    }

    private void validateSpotBookmarkable(Long spotId) {
        var spot = spotRepository.findByIdIncludingDeleted(spotId)
            .orElseThrow(() -> new BusinessException(SpotErrorCode.SPOT_NOT_FOUND));
        if (spot.getDeletedAt() != null) {
            throw new BusinessException(SpotErrorCode.SPOT_DELETED);
        }
    }

    private void validateSpotExists(Long spotId) {
        spotRepository.findByIdIncludingDeleted(spotId)
            .orElseThrow(() -> new BusinessException(SpotErrorCode.SPOT_NOT_FOUND));
    }

    private void insertArchive(Long userId, Long spotId) {
        try {
            savedSpotArchiveRepository.saveAndFlush(
                SavedSpotArchive.builder().userId(userId).spotId(spotId).build()
            );
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(SavedSpotErrorCode.ALREADY_BOOKMARKED);
        }
    }

    private long fetchBookmarkCount(Long spotId) {
        return spotRepository.findBookmarkCountById(spotId).orElse(0L);
    }

    private Map<Long, SpotImage> loadImageMap(List<Long> spotIds) {
        if (NullUtils.isEmpty(spotIds)) {
            return Collections.emptyMap();
        }
        return spotImageRepository.findAllBySpotIdIn(spotIds)
            .stream().collect(Collectors.toMap(SpotImage::getSpotId, image -> image));
    }

    // 비공개 전환된 스팟은 이미지를 내리지 않는다. 이름과 좌표는 어떤 스팟을 저장했는지 알 수 있게 남긴다.
    private SavedSpotItem toSavedSpotItem(SavedSpotRow row, Map<Long, SpotImage> imageMap) {
        boolean isPrivate = !SpotStatus.PUBLISHED.getCode().equals(row.status());
        String imageUrl = isPrivate ? null : resolveImageUrl(imageMap.get(row.spotId()));
        return new SavedSpotItem(
            row.spotId(), row.name(), row.theme(), imageUrl,
            row.latitude(), row.longitude(), row.distanceKm(), row.bookmarkCount(),
            row.savedAt(), row.deleted(), isPrivate
        );
    }

    private String resolveImageUrl(SpotImage spotImage) {
        return Optional.ofNullable(spotImage)
            .map(img -> storageService.getUrl(img.getImageKey()))
            .orElse(null);
    }
}
