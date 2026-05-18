package com.ioes.photo.domain.myspot.service;

import com.ioes.photo.domain.myspot.dto.MySpotListResponse;
import com.ioes.photo.domain.myspot.dto.MySpotListResponse.MySpotItem;
import com.ioes.photo.domain.myspot.mapper.MySpotMapper;
import com.ioes.photo.domain.myspot.mapper.MySpotRow;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.global.common.util.NullUtils;
import com.ioes.photo.global.storage.StorageService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 나만의 스팟(사용자가 등록한 스팟) 조회 서비스.
 *
 * 검수 대기(PENDING)와 공개(PUBLISHED) 상태를 모두 노출하며, 반려(REJECTED) 상태는 제외한다.
 *
 * @author 김성민
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MySpotService {

    private static final int PAGE_SIZE = 6;
    private static final List<String> VISIBLE_STATUS_CODES = List.of(
        SpotStatus.PENDING.getCode(),
        SpotStatus.PUBLISHED.getCode()
    );

    private final MySpotMapper mySpotMapper;
    private final SpotImageRepository spotImageRepository;
    private final StorageService storageService;

    public MySpotListResponse findMySpots(Long userId, int page, Double latitude, Double longitude) {
        List<MySpotRow> rows = mySpotMapper.findMySpots(
            userId, latitude, longitude, VISIBLE_STATUS_CODES, page * PAGE_SIZE, PAGE_SIZE
        );

        Map<Long, SpotImage> imageMap = loadImageMap(rows.stream().map(MySpotRow::spotId).toList());

        List<MySpotItem> items = rows.stream()
            .map(row -> toMySpotItem(row, imageMap))
            .toList();

        boolean hasNext = mySpotMapper.countMySpots(userId, VISIBLE_STATUS_CODES)
            > (long) (page + 1) * PAGE_SIZE;
        return new MySpotListResponse(items, page, hasNext);
    }

    private Map<Long, SpotImage> loadImageMap(List<Long> spotIds) {
        if (NullUtils.isEmpty(spotIds)) {
            return Collections.emptyMap();
        }
        return spotImageRepository.findAllBySpotIdIn(spotIds)
            .stream().collect(Collectors.toMap(SpotImage::getSpotId, image -> image));
    }

    private MySpotItem toMySpotItem(MySpotRow row, Map<Long, SpotImage> imageMap) {
        String imageUrl = resolveImageUrl(imageMap.get(row.spotId()));
        return new MySpotItem(
            row.spotId(), row.name(), row.theme(), imageUrl,
            row.latitude(), row.longitude(), row.distanceKm(), row.createdAt(),
            SpotStatus.fromCode(row.status()).name()
        );
    }

    private String resolveImageUrl(SpotImage spotImage) {
        return Optional.ofNullable(spotImage)
            .map(img -> storageService.getUrl(img.getImageKey()))
            .orElse(null);
    }
}
