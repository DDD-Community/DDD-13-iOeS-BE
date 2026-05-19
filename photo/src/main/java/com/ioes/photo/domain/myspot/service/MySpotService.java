package com.ioes.photo.domain.myspot.service;

import com.ioes.photo.domain.myspot.dto.CreateMySpotRequest;
import com.ioes.photo.domain.myspot.dto.CreateMySpotResponse;
import com.ioes.photo.domain.myspot.dto.MySpotListResponse;
import com.ioes.photo.domain.myspot.dto.MySpotListResponse.MySpotItem;
import com.ioes.photo.domain.myspot.mapper.MySpotMapper;
import com.ioes.photo.domain.myspot.mapper.MySpotRow;
import com.ioes.photo.domain.spot.dto.SpotImageSyncRequest;
import com.ioes.photo.domain.spot.dto.SpotImageSyncResponse;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spot.service.SpotImageAdminService;
import com.ioes.photo.global.common.util.NullUtils;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import com.ioes.photo.global.storage.StorageService;
import com.ioes.photo.global.storage.StorageUploadRollbackEvent;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 나만의 스팟(사용자가 등록한 스팟) 서비스.
 *
 * 조회: 사용자가 등록한 모든 상태(PENDING/PUBLISHED/REJECTED)의 스팟을 노출한다.
 * 등록: PENDING 상태로 저장하며, 클라이언트가 S3에 직접 업로드한 이미지 키를 받아 sync한다.
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
        SpotStatus.PUBLISHED.getCode(),
        SpotStatus.REJECTED.getCode()
    );

    private final MySpotMapper mySpotMapper;
    private final SpotImageRepository spotImageRepository;
    private final StorageService storageService;
    private final SpotRepository spotRepository;
    private final SpotImageAdminService spotImageAdminService;
    private final ApplicationEventPublisher eventPublisher;

    public MySpotListResponse findMySpots(Long userId, int page, Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "위도와 경도는 함께 입력해야 합니다.");
        }

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

    @Transactional
    public CreateMySpotResponse createMySpot(Long userId, CreateMySpotRequest request) {
        Spot spot = spotRepository.save(Spot.builder()
            .name(request.name())
            .comment(request.comment())
            .theme(request.theme())
            .latitude(request.latitude())
            .longitude(request.longitude())
            .address(request.address())
            .status(SpotStatus.PENDING)
            .userId(userId)
            .build());

        eventPublisher.publishEvent(new StorageUploadRollbackEvent(request.imageKey()));

        SpotImageSyncResponse sync = spotImageAdminService.syncImage(
            spot.getId(),
            new SpotImageSyncRequest(
                request.imageKey(),
                request.originalFilename(),
                request.contentType(),
                request.recordedDate(),
                request.recordedTime()
            )
        );

        return new CreateMySpotResponse(
            spot.getId(),
            SpotStatus.PENDING.name(),
            sync.imageUrl(),
            sync.thumbnailUrl()
        );
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
