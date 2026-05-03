package com.ioes.photo.domain.spot.service;

import com.ioes.photo.domain.spot.dto.SpotListResponse;
import com.ioes.photo.domain.spot.dto.SpotListResponse.SpotItem;
import com.ioes.photo.domain.spot.dto.SpotViewportResponse;
import com.ioes.photo.domain.spot.dto.SpotViewportResponse.SpotSummary;
import com.ioes.photo.domain.spot.dto.ViewportRequest;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.mapper.SpotMapper;
import com.ioes.photo.domain.spot.mapper.SpotRow;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    private final SpotThumbnailService spotThumbnailService;
    private final SpotMapper spotMapper;

    public SpotViewportResponse findSpotsInViewport(ViewportRequest request) {
        List<Spot> spots = spotRepository.findAllInViewport(
            request.minLat(), request.maxLat(),
            request.minLng(), request.maxLng(),
            SpotStatus.PUBLISHED.getCode()
        );

        Map<Long, SpotImage> imageMap = loadImageMap(spots.stream().map(Spot::getId).toList());

        List<SpotSummary> summaries = spots.stream()
            .map(spot -> toSpotSummary(spot, imageMap))
            .toList();

        return new SpotViewportResponse(summaries);
    }

    public SpotListResponse findSpots(int page, SpotTheme theme, Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "위도와 경도는 함께 입력해야 합니다.");
        }

        String status = SpotStatus.PUBLISHED.name();
        String themeStr = theme != null
                ? theme.name()
                : null;

        List<SpotRow> rows = spotMapper.findSpots(status, themeStr, latitude, longitude, page * LIST_PAGE_SIZE, LIST_PAGE_SIZE);
        Map<Long, SpotImage> imageMap = loadImageMap(rows.stream().map(SpotRow::id).toList());

        List<SpotItem> items = rows.stream()
            .map(row -> toSpotItem(row, imageMap))
            .toList();

        return new SpotListResponse(items, page, spotMapper.countSpots(status, themeStr) > (long) (page + 1) * LIST_PAGE_SIZE);
    }

    private Map<Long, SpotImage> loadImageMap(List<Long> spotIds) {
        if (spotIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return spotImageRepository.findAllBySpotIdIn(spotIds)
            .stream().collect(Collectors.toMap(SpotImage::getSpotId, image -> image));
    }

    private SpotSummary toSpotSummary(Spot spot, Map<Long, SpotImage> imageMap) {
        String thumbnailUrl = thumbnailUrl(imageMap.get(spot.getId()));
        return new SpotSummary(spot.getId(), thumbnailUrl, spot.getLatitude(), spot.getLongitude());
    }

    private SpotItem toSpotItem(SpotRow row, Map<Long, SpotImage> imageMap) {
        String thumbnailUrl = thumbnailUrl(imageMap.get(row.id()));
        return new SpotItem(row.id(), row.name(), row.theme(), thumbnailUrl, row.distanceKm());
    }

    private String thumbnailUrl(SpotImage spotImage) {
        return Optional.ofNullable(spotImage)
            .map(spotThumbnailService::getThumbnailUrl)
            .orElse(null);
    }
}