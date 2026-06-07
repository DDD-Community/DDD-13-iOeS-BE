package com.ioes.photo.domain.myspot.service;

import com.ioes.photo.domain.alarm.service.SpotAlarmService;
import com.ioes.photo.domain.myspot.dto.CreateMySpotRequest;
import com.ioes.photo.domain.myspot.dto.CreateMySpotResponse;
import com.ioes.photo.domain.myspot.dto.MySpotListResponse;
import com.ioes.photo.domain.myspot.dto.MySpotListResponse.MySpotItem;
import com.ioes.photo.domain.myspot.mapper.MySpotMapper;
import com.ioes.photo.domain.myspot.mapper.MySpotRow;
import com.ioes.photo.domain.crowdarea.service.CrowdAreaMapper;
import com.ioes.photo.domain.spot.dto.SpotImageSyncRequest;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.event.SpotCreatedEvent;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spot.service.SpotImageAdminService;
import com.ioes.photo.external.kakao.KakaoLocalApiClient;
import com.ioes.photo.external.kakao.dto.KakaoAddress;
import com.ioes.photo.external.weather.util.LccGridConverter;
import com.ioes.photo.external.weather.util.LccGridConverter.GridPoint;
import com.ioes.photo.global.common.util.NullUtils;
import com.ioes.photo.global.config.s3.properties.StorageProperties;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import com.ioes.photo.global.storage.AccessType;
import com.ioes.photo.global.storage.StoragePathUtils;
import com.ioes.photo.global.storage.StorageService;
import com.ioes.photo.global.storage.StorageUploadRollbackEvent;
import com.ioes.photo.global.storage.UploadResult;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 나만의 스팟(사용자가 등록한 스팟) 서비스.
 *
 * 조회: 사용자가 등록한 모든 상태(PENDING/PUBLISHED/REJECTED)의 스팟을 노출한다.
 * 등록: PENDING 상태로 저장하며, 업로드된 이미지를 서버가 S3에 저장하고 썸네일을 생성한다.
 *
 * @author 김성민
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MySpotService {

    private static final int PAGE_SIZE = 6;
    private static final String IMAGE_ENTITY = "spots";
    private static final String IMAGE_TYPE_ORIGINAL = "original";

    private final MySpotMapper mySpotMapper;
    private final SpotImageRepository spotImageRepository;
    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final SpotRepository spotRepository;
    private final SpotImageAdminService spotImageAdminService;
    private final KakaoLocalApiClient kakaoLocalApiClient;
    private final SpotAlarmService spotAlarmService;
    private final ApplicationEventPublisher eventPublisher;
    private final CrowdAreaMapper crowdAreaMapper;

    public MySpotListResponse findMySpots(Long userId, int page, Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "위도와 경도는 함께 입력해야 합니다.");
        }

        List<MySpotRow> rows = mySpotMapper.findMySpots(
            userId, latitude, longitude, page * PAGE_SIZE, PAGE_SIZE
        );

        Map<Long, SpotImage> imageMap = loadImageMap(rows.stream().map(MySpotRow::spotId).toList());

        List<MySpotItem> items = rows.stream()
            .map(row -> toMySpotItem(row, imageMap))
            .toList();

        boolean hasNext = mySpotMapper.countMySpots(userId)
            > (long) (page + 1) * PAGE_SIZE;
        return new MySpotListResponse(items, page, hasNext);
    }

    @Transactional
    public CreateMySpotResponse createMySpot(Long userId, CreateMySpotRequest request, MultipartFile image) {
        KakaoAddress address = resolveAddress(request.latitude(), request.longitude());
        GridPoint grid = LccGridConverter.toGrid(request.latitude(), request.longitude());

        Spot spot = spotRepository.save(Spot.builder()
            .name(request.name())
            .comment(request.comment())
            .theme(request.theme())
            .latitude(request.latitude())
            .longitude(request.longitude())
            .address(address == null ? null : address.simpleAddress())
            .addressRoad(address == null ? null : address.roadAddress())
            .addressJibun(address == null ? null : address.jibunAddress())
            .status(SpotStatus.PENDING)
            .gridNx(grid.nx())
            .gridNy(grid.ny())
            .crowdAreaName(crowdAreaMapper.findNearestAreaName(
                request.latitude(), request.longitude()).orElse(null))
            .userId(userId)
            .build());
        eventPublisher.publishEvent(new SpotCreatedEvent(spot.getId()));

        String imageKey = StoragePathUtils.generate(
            storageProperties.env(), AccessType.PUBLIC, IMAGE_ENTITY, spot.getId(),
            IMAGE_TYPE_ORIGINAL, image.getOriginalFilename());
        UploadResult upload = storageService.upload(image, imageKey);
        eventPublisher.publishEvent(new StorageUploadRollbackEvent(upload.key()));

        spotImageAdminService.syncImage(
            spot.getId(),
            new SpotImageSyncRequest(
                upload.key(),
                upload.originalFilename(),
                upload.contentType(),
                request.recordedDate(),
                request.recordedTime()
            )
        );

        spotAlarmService.subscribe(userId, spot.getId());

        return new CreateMySpotResponse(
            spot.getId(),
            SpotStatus.PENDING.name(),
            storageService.getUrl(upload.key())
        );
    }

    private KakaoAddress resolveAddress(double latitude, double longitude) {
        try {
            return kakaoLocalApiClient.reverseGeocode(latitude, longitude).orElse(null);
        } catch (BusinessException e) {
            log.warn("나만의 스팟 주소 역지오코딩 실패, 주소 없이 등록합니다. lat={}, lng={}, message={}",
                latitude, longitude, e.getMessage());
            return null;
        }
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
            SpotStatus.fromCode(row.status()).name(), row.bookmarkCount()
        );
    }

    private String resolveImageUrl(SpotImage spotImage) {
        return Optional.ofNullable(spotImage)
            .map(img -> storageService.getUrl(img.getImageKey()))
            .orElse(null);
    }
}
