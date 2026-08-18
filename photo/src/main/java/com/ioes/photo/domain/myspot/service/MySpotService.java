package com.ioes.photo.domain.myspot.service;

import com.ioes.photo.domain.alarm.service.SpotAlarmService;
import com.ioes.photo.domain.myspot.dto.CancelPublicationResponse;
import com.ioes.photo.domain.myspot.dto.CreateMySpotRequest;
import com.ioes.photo.domain.myspot.dto.CreateMySpotResponse;
import com.ioes.photo.domain.myspot.dto.MySpotListResponse;
import com.ioes.photo.domain.myspot.dto.MySpotListResponse.MySpotItem;
import com.ioes.photo.domain.myspot.dto.OpenMySpotResponse;
import com.ioes.photo.domain.myspot.dto.UpdateMySpotRequest;
import com.ioes.photo.domain.myspot.dto.UpdateMySpotResponse;
import com.ioes.photo.domain.myspot.mapper.MySpotMapper;
import com.ioes.photo.domain.myspot.mapper.MySpotRow;
import com.ioes.photo.domain.crowdarea.service.CrowdAreaMapper;
import com.ioes.photo.domain.spot.dto.SpotImageSyncRequest;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.entity.SpotOpenRequest;
import com.ioes.photo.domain.spot.enums.SpotOpenRequestStatus;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.error.SpotErrorCode;
import com.ioes.photo.domain.spot.event.SpotCreatedEvent;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.domain.spot.repository.SpotOpenRequestRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spot.service.SpotImageAccessService;
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
import com.ioes.photo.global.storage.StorageCleanupEvent;
import com.ioes.photo.global.storage.StorageService;
import com.ioes.photo.global.storage.StorageUploadRollbackEvent;
import com.ioes.photo.global.storage.UploadResult;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
 * 조회: 사용자가 등록한 모든 상태(DRAFT/PENDING/RE_REVIEW_PENDING/PUBLISHED/REJECTED)의 스팟을 노출한다.
 * 등록: DRAFT(나만보기) 상태로 저장하며, 업로드된 이미지를 서버가 S3에 저장하고 썸네일을 생성한다.
 * 검수는 사용자가 '오픈하기'를 눌러 오픈 신청한 시점(PENDING)부터 시작된다.
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
    private static final String ALREADY_RESOLVED_MESSAGE = "이미 처리된 신청이에요.";

    private final MySpotMapper mySpotMapper;
    private final SpotImageRepository spotImageRepository;
    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final SpotRepository spotRepository;
    private final SpotOpenRequestRepository spotOpenRequestRepository;
    private final SpotImageAdminService spotImageAdminService;
    private final SpotImageAccessService spotImageAccessService;
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
        GeoAttributes geo = resolveGeoAttributes(request.latitude(), request.longitude());
        KakaoAddress address = geo.address();

        Spot spot = spotRepository.save(Spot.builder()
            .name(request.name())
            .comment(request.comment())
            .theme(request.theme())
            .latitude(request.latitude())
            .longitude(request.longitude())
            .address(address == null ? null : address.simpleAddress())
            .addressRoad(address == null ? null : address.roadAddress())
            .addressJibun(address == null ? null : address.jibunAddress())
            .status(SpotStatus.DRAFT)
            .gridNx(geo.grid().nx())
            .gridNy(geo.grid().ny())
            .crowdAreaName(geo.crowdAreaName())
            .userId(userId)
            .build());
        eventPublisher.publishEvent(new SpotCreatedEvent(spot.getId()));

        UploadResult upload = uploadOriginalImage(spot.getId(), image);
        syncImage(spot.getId(), upload, request.recordedDate(), request.recordedTime());

        spotAlarmService.subscribe(userId, spot.getId());

        return new CreateMySpotResponse(
            spot.getId(),
            SpotStatus.DRAFT.name(),
            storageService.getUrl(upload.key())
        );
    }

    // 좌표가 그대로면 주소·격자·혼잡 지역을 다시 계산하지 않는다. 카카오 로컬 API 에 호출 쿼터가 있다.
    @Transactional
    public UpdateMySpotResponse updateMySpot(Long userId, Long spotId,
                                             UpdateMySpotRequest request, MultipartFile image) {
        Spot spot = findOwnedSpotWithLock(userId, spotId);
        if (!spot.isEditable()) {
            throw new BusinessException(SpotErrorCode.SPOT_NOT_EDITABLE);
        }

        spot.updateBasic(request.name(), request.comment(), request.theme());
        applyLocationChange(spot, request);

        String imageUrl = applyImageChange(spotId, request, image);
        return new UpdateMySpotResponse(spotId, spot.getStatus().name(), imageUrl);
    }

    // 카운터와 좋아요/북마크 기록은 건드리지 않는다. 되돌릴 수 없는 파괴적 변경이 되기 때문이다.
    @Transactional
    public void deleteMySpot(Long userId, Long spotId) {
        Spot spot = findOwnedSpotWithLock(userId, spotId);
        if (!spot.isDeletable()) {
            throw new BusinessException(SpotErrorCode.SPOT_NOT_DELETABLE);
        }

        spot.softDelete(LocalDateTime.now());
        spotAlarmService.disableBySpotId(spotId);
        cleanUpImages(spotId);
    }

    private void applyLocationChange(Spot spot, UpdateMySpotRequest request) {
        if (spot.isAt(request.latitude(), request.longitude())) {
            return;
        }

        GeoAttributes geo = resolveGeoAttributes(request.latitude(), request.longitude());
        KakaoAddress address = geo.address();
        spot.updateLocation(
            request.latitude(), request.longitude(),
            address == null ? null : address.simpleAddress(),
            address == null ? null : address.roadAddress(),
            address == null ? null : address.jibunAddress()
        );
        spot.assignGrid(geo.grid().nx(), geo.grid().ny());
        spot.assignCrowdAreaName(geo.crowdAreaName());

        // 좌표가 바뀌었으니 날씨/일몰/혼잡도를 새 위치 기준으로 다시 수집한다.
        eventPublisher.publishEvent(new SpotCreatedEvent(spot.getId()));
    }

    private String applyImageChange(Long spotId, UpdateMySpotRequest request, MultipartFile image) {
        SpotImage current = spotImageRepository.findById(spotId).orElse(null);

        if (image == null || image.isEmpty()) {
            updateRecordedAt(current, request);
            return resolveImageUrl(current);
        }

        // syncImage 가 키를 덮어쓰므로 이전 키를 먼저 확보한다.
        String previousImageKey = current == null ? null : current.getImageKey();
        String previousThumbnailKey = current == null ? null : current.getThumbnailKey();

        UploadResult upload = uploadOriginalImage(spotId, image);
        syncImage(spotId, upload, request.recordedDate(), request.recordedTime());

        publishCleanup(previousImageKey);
        publishCleanup(previousThumbnailKey);

        return storageService.getUrl(upload.key());
    }

    private void updateRecordedAt(SpotImage image, UpdateMySpotRequest request) {
        if (image == null) {
            return;
        }
        image.updateRecordedDate(request.recordedDate());
        image.updateRecordedTime(request.recordedTime());
    }

    private void cleanUpImages(Long spotId) {
        spotImageRepository.findById(spotId).ifPresent(image -> {
            publishCleanup(image.getImageKey());
            publishCleanup(image.getThumbnailKey());
        });
    }

    private void publishCleanup(String key) {
        if (NullUtils.isNotBlank(key)) {
            eventPublisher.publishEvent(new StorageCleanupEvent(key));
        }
    }

    private UploadResult uploadOriginalImage(Long spotId, MultipartFile image) {
        String imageKey = StoragePathUtils.generate(
            storageProperties.env(), AccessType.PRIVATE, IMAGE_ENTITY, spotId,
            IMAGE_TYPE_ORIGINAL, image.getOriginalFilename());
        UploadResult upload = storageService.upload(image, imageKey);
        eventPublisher.publishEvent(new StorageUploadRollbackEvent(upload.key()));
        return upload;
    }

    private void syncImage(Long spotId, UploadResult upload, LocalDate recordedDate, LocalTime recordedTime) {
        spotImageAdminService.syncImage(
            spotId,
            new SpotImageSyncRequest(
                upload.key(),
                upload.originalFilename(),
                upload.contentType(),
                recordedDate,
                recordedTime
            )
        );
    }

    private GeoAttributes resolveGeoAttributes(double latitude, double longitude) {
        return new GeoAttributes(
            resolveAddress(latitude, longitude),
            LccGridConverter.toGrid(latitude, longitude),
            crowdAreaMapper.findNearestAreaName(latitude, longitude).orElse(null)
        );
    }

    private record GeoAttributes(KakaoAddress address, GridPoint grid, String crowdAreaName) {}

    @Transactional
    public OpenMySpotResponse requestOpen(Long userId, Long spotId) {
        Spot spot = findOwnedSpotWithLock(userId, spotId);

        if (!spot.isOpenRequestable()) {
            throw new BusinessException(SpotErrorCode.SPOT_NOT_OPENABLE);
        }

        LocalDateTime now = LocalDateTime.now();
        spot.requestOpen(now);
        spotOpenRequestRepository.save(SpotOpenRequest.request(spotId, userId, now));

        return new OpenMySpotResponse(spot.getId(), spot.getStatus().name());
    }

    // 좋아요/북마크는 그대로 둔다. 다시 공개됐을 때 이전에 쌓인 반응이 살아 있어야 한다.
    @Transactional
    public CancelPublicationResponse cancelPublication(Long userId, Long spotId) {
        Spot spot = findOwnedSpotWithLock(userId, spotId);
        SpotStatus previous = spot.getStatus();

        if (previous == SpotStatus.DRAFT) {
            throw new BusinessException(SpotErrorCode.SPOT_NOT_CANCELABLE);
        }
        if (!spot.isPublicationCancelable()) {
            // 철회를 누르기 직전에 운영자 검수가 먼저 확정된 경우다.
            throw new BusinessException(SpotErrorCode.SPOT_ALREADY_REVIEWED, ALREADY_RESOLVED_MESSAGE);
        }

        spot.cancelPublication();
        spotOpenRequestRepository
            .findFirstBySpotIdAndStatusOrderByRequestedAtDesc(spotId, SpotOpenRequestStatus.REQUESTED)
            .ifPresent(openRequest -> openRequest.cancel(LocalDateTime.now()));

        if (previous == SpotStatus.PUBLISHED) {
            spotImageAccessService.unpublish(spotId);
        }

        return new CancelPublicationResponse(spotId, previous.name(), spot.getStatus().name());
    }

    private Spot findOwnedSpotWithLock(Long userId, Long spotId) {
        Spot spot = spotRepository.findWithLockById(spotId)
            .orElseThrow(() -> new BusinessException(SpotErrorCode.SPOT_NOT_FOUND));

        if (!spot.isOwnedBy(userId)) {
            throw new BusinessException(SpotErrorCode.SPOT_ACCESS_DENIED);
        }
        return spot;
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
