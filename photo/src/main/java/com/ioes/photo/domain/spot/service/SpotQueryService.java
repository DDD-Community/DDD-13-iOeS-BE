package com.ioes.photo.domain.spot.service;

import com.ioes.photo.domain.spot.dto.SpotViewportResponse;
import com.ioes.photo.domain.spot.dto.SpotViewportResponse.SpotSummary;
import com.ioes.photo.domain.spot.dto.ViewportRequest;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import java.util.List;
import java.util.Map;
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

    private final SpotRepository spotRepository;
    private final SpotImageRepository spotImageRepository;
    private final SpotThumbnailService spotThumbnailService;

    public SpotViewportResponse findSpotsInViewport(ViewportRequest request) {
        List<Spot> spots = spotRepository.findAllInViewport(
            request.minLat(), request.maxLat(),
            request.minLng(), request.maxLng(),
            SpotStatus.PUBLISHED
        );

        List<Long> spotIds = spots.stream().map(Spot::getId).toList();
        Map<Long, SpotImage> imageBySpotId = spotImageRepository.findAllBySpotIdIn(spotIds)
            .stream().collect(Collectors.toMap(SpotImage::getSpotId, image -> image));

        List<SpotSummary> summaries = spots.stream()
            .map(spot -> {
                SpotImage spotImage = imageBySpotId.get(spot.getId());
                String thumbnailUrl = spotImage != null
                    ? spotThumbnailService.getThumbnailUrl(spotImage)
                    : null;
                return new SpotSummary(spot.getId(), thumbnailUrl, spot.getLatitude(), spot.getLongitude());
            })
            .toList();

        return new SpotViewportResponse(summaries);
    }
}
