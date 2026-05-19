package com.ioes.photo.domain.myspot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ioes.photo.domain.myspot.dto.CreateMySpotRequest;
import com.ioes.photo.domain.myspot.dto.CreateMySpotResponse;
import com.ioes.photo.domain.myspot.dto.MySpotListResponse;
import com.ioes.photo.domain.myspot.mapper.MySpotMapper;
import com.ioes.photo.domain.myspot.mapper.MySpotRow;
import com.ioes.photo.domain.spot.dto.SpotImageSyncRequest;
import com.ioes.photo.domain.spot.dto.SpotImageSyncResponse;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spot.service.SpotImageAdminService;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import com.ioes.photo.global.storage.StorageService;
import com.ioes.photo.global.storage.StorageUploadRollbackEvent;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * {@link MySpotService} 단위 테스트.
 *
 * @author 김성민
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MySpotService 단위 테스트")
class MySpotServiceTest {

    @Mock MySpotMapper mySpotMapper;
    @Mock SpotImageRepository spotImageRepository;
    @Mock StorageService storageService;
    @Mock SpotRepository spotRepository;
    @Mock SpotImageAdminService spotImageAdminService;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks MySpotService mySpotService;

    private static final Long USER_ID = 1L;
    private static final Long SPOT_ID = 10L;
    private static final List<String> VISIBLE_CODES = List.of(
        SpotStatus.PENDING.getCode(),
        SpotStatus.PUBLISHED.getCode(),
        SpotStatus.REJECTED.getCode()
    );

    @Nested
    @DisplayName("findMySpots()")
    class FindMySpots {

        @Test
        @DisplayName("위도/경도 모두 null이면 distanceKm 없이 정상 처리된다")
        void successWhenBothNull() {
            given(mySpotMapper.findMySpots(USER_ID, null, null, VISIBLE_CODES, 0, 6)).willReturn(List.of());
            given(mySpotMapper.countMySpots(USER_ID, VISIBLE_CODES)).willReturn(0L);

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, null, null);

            assertThat(response.spots()).isEmpty();
            assertThat(response.page()).isZero();
            assertThat(response.hasNext()).isFalse();
        }

        @Test
        @DisplayName("위도만 전달되면 INVALID_INPUT_VALUE 예외를 던진다")
        void throwsWhenOnlyLatProvided() {
            assertThatThrownBy(() -> mySpotService.findMySpots(USER_ID, 0, 37.5, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.INVALID_INPUT_VALUE);
        }

        @Test
        @DisplayName("경도만 전달되면 INVALID_INPUT_VALUE 예외를 던진다")
        void throwsWhenOnlyLngProvided() {
            assertThatThrownBy(() -> mySpotService.findMySpots(USER_ID, 0, null, 127.0))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.INVALID_INPUT_VALUE);
        }

        @Test
        @DisplayName("조회된 스팟의 imageUrl은 StorageService.getUrl 결과다")
        void imageUrlIsFromStorageService() {
            MySpotRow row = buildRow(SPOT_ID, SpotStatus.PUBLISHED.getCode());
            SpotImage image = SpotImage.create(SPOT_ID, "spots/1/image.jpg");
            given(mySpotMapper.findMySpots(USER_ID, null, null, VISIBLE_CODES, 0, 6)).willReturn(List.of(row));
            given(mySpotMapper.countMySpots(USER_ID, VISIBLE_CODES)).willReturn(1L);
            given(spotImageRepository.findAllBySpotIdIn(List.of(SPOT_ID))).willReturn(List.of(image));
            given(storageService.getUrl("spots/1/image.jpg")).willReturn("https://cdn.example.com/image.jpg");

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, null, null);

            assertThat(response.spots()).hasSize(1);
            assertThat(response.spots().get(0).imageUrl()).isEqualTo("https://cdn.example.com/image.jpg");
        }

        @Test
        @DisplayName("이미지가 없는 스팟의 imageUrl은 null이다")
        void imageUrlIsNullWhenNoImage() {
            MySpotRow row = buildRow(SPOT_ID, SpotStatus.PUBLISHED.getCode());
            given(mySpotMapper.findMySpots(USER_ID, null, null, VISIBLE_CODES, 0, 6)).willReturn(List.of(row));
            given(mySpotMapper.countMySpots(USER_ID, VISIBLE_CODES)).willReturn(1L);
            given(spotImageRepository.findAllBySpotIdIn(List.of(SPOT_ID))).willReturn(List.of());

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, null, null);

            assertThat(response.spots().get(0).imageUrl()).isNull();
        }

        @Test
        @DisplayName("PENDING 상태 스팟의 status는 enum 이름(PENDING)으로 변환되어 응답된다")
        void includesPendingSpot_withEnumName() {
            MySpotRow row = buildRow(SPOT_ID, SpotStatus.PENDING.getCode());
            given(mySpotMapper.findMySpots(USER_ID, null, null, VISIBLE_CODES, 0, 6)).willReturn(List.of(row));
            given(mySpotMapper.countMySpots(USER_ID, VISIBLE_CODES)).willReturn(1L);
            given(spotImageRepository.findAllBySpotIdIn(List.of(SPOT_ID))).willReturn(List.of());

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, null, null);

            assertThat(response.spots().get(0).status()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("PUBLISHED 상태 스팟의 status는 PUBLISHED 이름으로 변환된다")
        void includesPublishedSpot_withEnumName() {
            MySpotRow row = buildRow(SPOT_ID, SpotStatus.PUBLISHED.getCode());
            given(mySpotMapper.findMySpots(USER_ID, null, null, VISIBLE_CODES, 0, 6)).willReturn(List.of(row));
            given(mySpotMapper.countMySpots(USER_ID, VISIBLE_CODES)).willReturn(1L);
            given(spotImageRepository.findAllBySpotIdIn(List.of(SPOT_ID))).willReturn(List.of());

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, null, null);

            assertThat(response.spots().get(0).status()).isEqualTo("PUBLISHED");
        }

        @Test
        @DisplayName("REJECTED 상태 스팟의 status는 REJECTED 이름으로 변환된다")
        void includesRejectedSpot_withEnumName() {
            MySpotRow row = buildRow(SPOT_ID, SpotStatus.REJECTED.getCode());
            given(mySpotMapper.findMySpots(USER_ID, null, null, VISIBLE_CODES, 0, 6)).willReturn(List.of(row));
            given(mySpotMapper.countMySpots(USER_ID, VISIBLE_CODES)).willReturn(1L);
            given(spotImageRepository.findAllBySpotIdIn(List.of(SPOT_ID))).willReturn(List.of());

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, null, null);

            assertThat(response.spots().get(0).status()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("전체 개수가 다음 페이지 기준 초과 시 hasNext가 true다")
        void hasNextTrue_whenMorePages() {
            given(mySpotMapper.findMySpots(USER_ID, null, null, VISIBLE_CODES, 0, 6)).willReturn(List.of());
            given(mySpotMapper.countMySpots(USER_ID, VISIBLE_CODES)).willReturn(7L);

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, null, null);

            assertThat(response.hasNext()).isTrue();
        }

        @Test
        @DisplayName("전체 개수가 다음 페이지 기준 이하면 hasNext가 false다")
        void hasNextFalse_whenLastPage() {
            given(mySpotMapper.findMySpots(USER_ID, null, null, VISIBLE_CODES, 0, 6)).willReturn(List.of());
            given(mySpotMapper.countMySpots(USER_ID, VISIBLE_CODES)).willReturn(6L);

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, null, null);

            assertThat(response.hasNext()).isFalse();
        }

        @Test
        @DisplayName("page=1 요청 시 offset=6으로 매퍼에 전달된다")
        void passesCorrectOffset_forSecondPage() {
            given(mySpotMapper.findMySpots(USER_ID, null, null, VISIBLE_CODES, 6, 6)).willReturn(List.of());
            given(mySpotMapper.countMySpots(USER_ID, VISIBLE_CODES)).willReturn(0L);

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 1, null, null);

            assertThat(response.page()).isEqualTo(1);
        }
    }

    private MySpotRow buildRow(Long spotId, String statusCode) {
        return new MySpotRow(spotId, "테스트스팟", "SS", 37.5, 127.0, null, LocalDateTime.now(), statusCode);
    }

    @Nested
    @DisplayName("createMySpot()")
    class CreateMySpot {

        private static final String IMAGE_KEY = "prod/public/spots/temp/original/202605/photo.jpg";

        private CreateMySpotRequest request() {
            return new CreateMySpotRequest(
                "한강 노을 명소",
                SpotTheme.SUNSET,
                37.5326,
                126.9905,
                "코멘트",
                "서울시 영등포구",
                IMAGE_KEY,
                "photo.jpg",
                "image/jpeg",
                null,
                null
            );
        }

        private Spot savedSpot(Long id) {
            Spot spot = Spot.builder()
                .name("한강 노을 명소")
                .theme(SpotTheme.SUNSET)
                .latitude(37.5326)
                .longitude(126.9905)
                .userId(USER_ID)
                .build();
            try {
                Field idField = spot.getClass().getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(spot, id);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            return spot;
        }

        @Test
        @DisplayName("Spot을 PENDING 상태와 userId로 저장한다")
        void savesSpotAsPendingWithUserId() {
            given(spotRepository.save(any(Spot.class))).willAnswer(inv -> {
                Spot s = inv.getArgument(0);
                Field idField = s.getClass().getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(s, SPOT_ID);
                return s;
            });
            given(spotImageAdminService.syncImage(any(), any()))
                .willReturn(new SpotImageSyncResponse("https://cdn/img", "https://cdn/thumb"));

            mySpotService.createMySpot(USER_ID, request());

            ArgumentCaptor<Spot> captor = ArgumentCaptor.forClass(Spot.class);
            then(spotRepository).should().save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(SpotStatus.PENDING);
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
            assertThat(captor.getValue().getName()).isEqualTo("한강 노을 명소");
            assertThat(captor.getValue().getTheme()).isEqualTo(SpotTheme.SUNSET);
        }

        @Test
        @DisplayName("저장된 spotId와 imageKey로 spotImageAdminService.syncImage를 호출한다")
        void delegatesImageSyncWithSavedSpotIdAndImageKey() {
            given(spotRepository.save(any(Spot.class))).willReturn(savedSpot(SPOT_ID));
            given(spotImageAdminService.syncImage(any(), any()))
                .willReturn(new SpotImageSyncResponse("https://cdn/img", "https://cdn/thumb"));

            mySpotService.createMySpot(USER_ID, request());

            ArgumentCaptor<SpotImageSyncRequest> reqCaptor = ArgumentCaptor.forClass(SpotImageSyncRequest.class);
            then(spotImageAdminService).should().syncImage(org.mockito.ArgumentMatchers.eq(SPOT_ID), reqCaptor.capture());
            assertThat(reqCaptor.getValue().imageKey()).isEqualTo(IMAGE_KEY);
            assertThat(reqCaptor.getValue().originalFilename()).isEqualTo("photo.jpg");
            assertThat(reqCaptor.getValue().contentType()).isEqualTo("image/jpeg");
        }

        @Test
        @DisplayName("이미지 sync 응답을 그대로 응답 DTO에 매핑한다")
        void mapsSyncResponseIntoResult() {
            given(spotRepository.save(any(Spot.class))).willReturn(savedSpot(SPOT_ID));
            given(spotImageAdminService.syncImage(any(), any()))
                .willReturn(new SpotImageSyncResponse("https://cdn/img", "https://cdn/thumb"));

            CreateMySpotResponse response = mySpotService.createMySpot(USER_ID, request());

            assertThat(response.spotId()).isEqualTo(SPOT_ID);
            assertThat(response.status()).isEqualTo(SpotStatus.PENDING.name());
            assertThat(response.imageUrl()).isEqualTo("https://cdn/img");
            assertThat(response.thumbnailUrl()).isEqualTo("https://cdn/thumb");
        }

        @Test
        @DisplayName("등록 직후 imageKey에 대한 StorageUploadRollbackEvent를 발행한다")
        void publishesUploadRollbackEvent() {
            given(spotRepository.save(any(Spot.class))).willReturn(savedSpot(SPOT_ID));
            given(spotImageAdminService.syncImage(any(), any()))
                .willReturn(new SpotImageSyncResponse("https://cdn/img", "https://cdn/thumb"));

            mySpotService.createMySpot(USER_ID, request());

            ArgumentCaptor<StorageUploadRollbackEvent> evCaptor =
                ArgumentCaptor.forClass(StorageUploadRollbackEvent.class);
            then(eventPublisher).should().publishEvent(evCaptor.capture());
            assertThat(evCaptor.getValue().key()).isEqualTo(IMAGE_KEY);
        }
    }
}
