package com.ioes.photo.domain.myspot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ioes.photo.domain.alarm.service.SpotAlarmService;
import com.ioes.photo.domain.crowdarea.service.CrowdAreaMapper;
import com.ioes.photo.domain.myspot.dto.UpdateMySpotRequest;
import com.ioes.photo.domain.myspot.dto.UpdateMySpotResponse;
import com.ioes.photo.domain.myspot.mapper.MySpotMapper;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.error.SpotErrorCode;
import com.ioes.photo.domain.spot.event.SpotCreatedEvent;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.domain.spot.repository.SpotOpenRequestRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spot.service.SpotImageAccessService;
import com.ioes.photo.domain.spot.service.SpotImageAdminService;
import com.ioes.photo.external.kakao.KakaoLocalApiClient;
import com.ioes.photo.external.kakao.dto.KakaoAddress;
import com.ioes.photo.global.config.s3.properties.StorageProperties;
import com.ioes.photo.global.error.exception.BusinessException;
import com.ioes.photo.global.storage.StorageCleanupEvent;
import com.ioes.photo.global.storage.StorageService;
import com.ioes.photo.global.storage.UploadResult;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@link MySpotService} 의 수정/삭제 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MySpotService 수정/삭제 단위 테스트")
class MySpotServiceUpdateDeleteTest {

    @Mock MySpotMapper mySpotMapper;
    @Mock SpotImageRepository spotImageRepository;
    @Mock StorageService storageService;
    @Mock StorageProperties storageProperties;
    @Mock SpotRepository spotRepository;
    @Mock SpotOpenRequestRepository spotOpenRequestRepository;
    @Mock SpotImageAdminService spotImageAdminService;
    @Mock SpotImageAccessService spotImageAccessService;
    @Mock KakaoLocalApiClient kakaoLocalApiClient;
    @Mock SpotAlarmService spotAlarmService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock CrowdAreaMapper crowdAreaMapper;

    @InjectMocks MySpotService mySpotService;

    private static final Long USER_ID = 1L;
    private static final Long SPOT_ID = 10L;
    private static final double LAT = 37.5326;
    private static final double LNG = 126.9905;
    private static final String OLD_IMAGE_KEY = "dev/private/spots/10/original/202605/old.jpg";
    private static final String OLD_THUMB_KEY = "dev/private/spots/10/thumbnail/202605/old.jpg";
    private static final String NEW_IMAGE_KEY = "dev/private/spots/10/original/202608/new.jpg";

    @Nested
    @DisplayName("updateMySpot()")
    class UpdateMySpot {

        @Test
        @DisplayName("이름/코멘트/테마를 전달한 값으로 덮어쓰고 상태는 유지한다")
        void updatesBasicFieldsKeepingStatus() {
            Spot spot = ownedSpot(SpotStatus.REJECTED);
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(spot));
            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.empty());

            UpdateMySpotResponse response = mySpotService.updateMySpot(USER_ID, SPOT_ID, request(), null);

            assertThat(spot.getName()).isEqualTo("수정된 이름");
            assertThat(spot.getComment()).isEqualTo("수정된 코멘트");
            assertThat(spot.getTheme()).isEqualTo(SpotTheme.NIGHT_VIEW);
            assertThat(spot.getStatus()).isEqualTo(SpotStatus.REJECTED);
            assertThat(response.status()).isEqualTo(SpotStatus.REJECTED.name());
        }

        @Test
        @DisplayName("좌표가 그대로면 외부 지오코딩과 날씨 재수집을 호출하지 않는다")
        void skipsGeoResolutionWhenLocationUnchanged() {
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(ownedSpot(SpotStatus.DRAFT)));
            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.empty());

            mySpotService.updateMySpot(USER_ID, SPOT_ID, request(), null);

            then(kakaoLocalApiClient).should(never()).reverseGeocode(anyDouble(), anyDouble());
            then(crowdAreaMapper).should(never()).findNearestAreaName(anyDouble(), anyDouble());
            then(eventPublisher).should(never()).publishEvent(any(SpotCreatedEvent.class));
        }

        @Test
        @DisplayName("좌표가 바뀌면 주소·격자·혼잡지역을 다시 계산하고 날씨 재수집을 요청한다")
        void recalculatesGeoAttributesWhenLocationChanged() {
            Spot spot = ownedSpot(SpotStatus.DRAFT);
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(spot));
            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.empty());
            given(kakaoLocalApiClient.reverseGeocode(37.6, 127.1))
                .willReturn(Optional.of(
                    new KakaoAddress("서울시 성동구 왕십리로 1", "서울시 성동구 성수동 1", "서울시 성동구")));
            given(crowdAreaMapper.findNearestAreaName(37.6, 127.1)).willReturn(Optional.of("성수카페거리"));

            mySpotService.updateMySpot(USER_ID, SPOT_ID, movedRequest(), null);

            assertThat(spot.getLatitude()).isEqualTo(37.6);
            assertThat(spot.getLongitude()).isEqualTo(127.1);
            assertThat(spot.getLocation().getX()).isEqualTo(127.1);
            assertThat(spot.getLocation().getY()).isEqualTo(37.6);
            assertThat(spot.getAddress()).isEqualTo("서울시 성동구");
            assertThat(spot.getAddressRoad()).isEqualTo("서울시 성동구 왕십리로 1");
            assertThat(spot.getAddressJibun()).isEqualTo("서울시 성동구 성수동 1");
            assertThat(spot.getCrowdAreaName()).isEqualTo("성수카페거리");
            assertThat(spot.getGridNx()).isNotNull();
            then(eventPublisher).should().publishEvent(any(SpotCreatedEvent.class));
        }

        @Test
        @DisplayName("이미지를 첨부하지 않으면 업로드하지 않고 촬영 일시만 갱신한다")
        void keepsExistingImageWhenNoFileAttached() {
            SpotImage image = SpotImage.create(SPOT_ID, OLD_IMAGE_KEY);
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(ownedSpot(SpotStatus.DRAFT)));
            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.of(image));
            given(storageService.getUrl(OLD_IMAGE_KEY)).willReturn("https://cdn.example.com/old.jpg");

            UpdateMySpotResponse response = mySpotService.updateMySpot(USER_ID, SPOT_ID, request(), null);

            then(storageService).should(never()).upload(any(), anyString());
            then(spotImageAdminService).should(never()).syncImage(anyLong(), any());
            assertThat(image.getRecordedDate()).isEqualTo(LocalDate.of(2026, 8, 1));
            assertThat(image.getRecordedTime()).isEqualTo(LocalTime.of(19, 0));
            assertThat(response.imageUrl()).isEqualTo("https://cdn.example.com/old.jpg");
        }

        @Test
        @DisplayName("이미지를 교체하면 새로 업로드하고 이전 원본/썸네일 정리를 예약한다")
        void replacesImageAndSchedulesOldKeyCleanup() {
            SpotImage image = SpotImage.create(SPOT_ID, OLD_IMAGE_KEY);
            image.updateThumbnailKey(OLD_THUMB_KEY);
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(ownedSpot(SpotStatus.DRAFT)));
            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.of(image));
            given(storageProperties.env()).willReturn("dev");
            given(storageService.upload(any(), anyString()))
                .willReturn(new UploadResult(NEW_IMAGE_KEY, "new.jpg", 100L, "image/jpeg"));
            given(storageService.getUrl(NEW_IMAGE_KEY)).willReturn("https://cdn.example.com/new.jpg");

            UpdateMySpotResponse response = mySpotService.updateMySpot(USER_ID, SPOT_ID, request(), imageFile());

            then(spotImageAdminService).should().syncImage(anyLong(), any());
            then(eventPublisher).should().publishEvent(new StorageCleanupEvent(OLD_IMAGE_KEY));
            then(eventPublisher).should().publishEvent(new StorageCleanupEvent(OLD_THUMB_KEY));
            assertThat(response.imageUrl()).isEqualTo("https://cdn.example.com/new.jpg");
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = SpotStatus.class, names = {"PENDING", "RE_REVIEW_PENDING", "PUBLISHED"})
        @DisplayName("검수중이거나 공개된 스팟은 SPOT_NOT_EDITABLE 예외를 던진다")
        void throwsWhenNotEditable(SpotStatus status) {
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(ownedSpot(status)));

            assertThatThrownBy(() -> mySpotService.updateMySpot(USER_ID, SPOT_ID, request(), null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SpotErrorCode.SPOT_NOT_EDITABLE);
        }

        @Test
        @DisplayName("본인 스팟이 아니면 SPOT_ACCESS_DENIED 예외를 던진다")
        void throwsWhenNotOwner() {
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(ownedSpot(SpotStatus.DRAFT)));

            assertThatThrownBy(() -> mySpotService.updateMySpot(999L, SPOT_ID, request(), null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SpotErrorCode.SPOT_ACCESS_DENIED);
        }

        @Test
        @DisplayName("존재하지 않는 스팟이면 SPOT_NOT_FOUND 예외를 던진다")
        void throwsWhenNotFound() {
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> mySpotService.updateMySpot(USER_ID, SPOT_ID, request(), null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SpotErrorCode.SPOT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("deleteMySpot()")
    class DeleteMySpot {

        @Test
        @DisplayName("삭제하면 deletedAt이 기록되고 알림 구독이 꺼지며 이미지 정리가 예약된다")
        void softDeletesAndCleansUp() {
            Spot spot = ownedSpot(SpotStatus.PUBLISHED);
            SpotImage image = SpotImage.create(SPOT_ID, OLD_IMAGE_KEY);
            image.updateThumbnailKey(OLD_THUMB_KEY);
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(spot));
            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.of(image));

            mySpotService.deleteMySpot(USER_ID, SPOT_ID);

            assertThat(spot.getDeletedAt()).isNotNull();
            assertThat(spot.isDeleted()).isTrue();
            then(spotAlarmService).should().disableBySpotId(SPOT_ID);
            then(eventPublisher).should().publishEvent(new StorageCleanupEvent(OLD_IMAGE_KEY));
            then(eventPublisher).should().publishEvent(new StorageCleanupEvent(OLD_THUMB_KEY));
        }

        @Test
        @DisplayName("삭제해도 좋아요/북마크 카운터는 건드리지 않는다")
        void keepsCountersUntouched() {
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(ownedSpot(SpotStatus.PUBLISHED)));
            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.empty());

            mySpotService.deleteMySpot(USER_ID, SPOT_ID);

            then(spotRepository).should(never()).decrementLikeCount(anyLong());
            then(spotRepository).should(never()).decrementBookmarkCount(anyLong());
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = SpotStatus.class, names = {"PENDING", "RE_REVIEW_PENDING"})
        @DisplayName("검수 중인 스팟은 SPOT_NOT_DELETABLE 예외를 던진다")
        void throwsWhenUnderReview(SpotStatus status) {
            Spot spot = ownedSpot(status);
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(spot));

            assertThatThrownBy(() -> mySpotService.deleteMySpot(USER_ID, SPOT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SpotErrorCode.SPOT_NOT_DELETABLE);

            assertThat(spot.getDeletedAt()).isNull();
            then(spotAlarmService).should(never()).disableBySpotId(anyLong());
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = SpotStatus.class, names = {"DRAFT", "PUBLISHED", "REJECTED"})
        @DisplayName("검수 중이 아니면 어떤 상태든 삭제할 수 있다")
        void allowsDeleteWhenNotUnderReview(SpotStatus status) {
            Spot spot = ownedSpot(status);
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(spot));
            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.empty());

            mySpotService.deleteMySpot(USER_ID, SPOT_ID);

            assertThat(spot.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("본인 스팟이 아니면 SPOT_ACCESS_DENIED 예외를 던진다")
        void throwsWhenNotOwner() {
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(ownedSpot(SpotStatus.PUBLISHED)));

            assertThatThrownBy(() -> mySpotService.deleteMySpot(999L, SPOT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SpotErrorCode.SPOT_ACCESS_DENIED);
        }
    }

    private static UpdateMySpotRequest request() {
        return new UpdateMySpotRequest("수정된 이름", SpotTheme.NIGHT_VIEW, LAT, LNG, "수정된 코멘트",
            LocalDate.of(2026, 8, 1), LocalTime.of(19, 0));
    }

    private static UpdateMySpotRequest movedRequest() {
        return new UpdateMySpotRequest("수정된 이름", SpotTheme.NIGHT_VIEW, 37.6, 127.1, "수정된 코멘트",
            LocalDate.of(2026, 8, 1), LocalTime.of(19, 0));
    }

    private static MultipartFile imageFile() {
        return new MockMultipartFile("image", "new.jpg", "image/jpeg", "binary".getBytes());
    }

    private static Spot ownedSpot(SpotStatus status) {
        Spot spot = Spot.builder()
            .name("원래 이름")
            .comment("원래 코멘트")
            .theme(SpotTheme.SUNSET)
            .latitude(LAT)
            .longitude(LNG)
            .status(status)
            .userId(USER_ID)
            .build();
        ReflectionTestUtils.setField(spot, "id", SPOT_ID);
        return spot;
    }
}
