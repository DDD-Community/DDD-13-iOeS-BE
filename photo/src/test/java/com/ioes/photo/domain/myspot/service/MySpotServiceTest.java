package com.ioes.photo.domain.myspot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ioes.photo.domain.alarm.service.SpotAlarmService;
import com.ioes.photo.domain.crowdarea.service.CrowdAreaMapper;
import com.ioes.photo.domain.spotregion.service.SpotRegionResolver;
import com.ioes.photo.domain.myspot.dto.CreateMySpotRequest;
import com.ioes.photo.domain.myspot.dto.CreateMySpotResponse;
import com.ioes.photo.domain.myspot.dto.MySpotListResponse;
import com.ioes.photo.domain.myspot.mapper.MySpotMapper;
import com.ioes.photo.domain.myspot.mapper.MySpotRow;
import com.ioes.photo.domain.spot.dto.SpotImageSyncRequest;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.entity.SpotOpenRequest;
import com.ioes.photo.domain.spot.enums.SpotOpenRequestStatus;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.error.SpotErrorCode;
import com.ioes.photo.domain.spot.event.SpotCreatedEvent;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.domain.spot.repository.SpotOpenRequestRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spot.service.SpotImageAccessService;
import com.ioes.photo.domain.spot.service.SpotImageAdminService;
import com.ioes.photo.external.error.ExternalApiErrorCode;
import com.ioes.photo.external.kakao.KakaoLocalApiClient;
import com.ioes.photo.external.kakao.dto.KakaoAddress;
import com.ioes.photo.external.weather.util.LccGridConverter;
import com.ioes.photo.external.weather.util.LccGridConverter.GridPoint;
import com.ioes.photo.global.config.s3.properties.StorageProperties;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import com.ioes.photo.global.storage.StorageService;
import com.ioes.photo.global.storage.StorageUploadRollbackEvent;
import com.ioes.photo.global.storage.UploadResult;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

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
    @Mock StorageProperties storageProperties;
    @Mock SpotRepository spotRepository;
    @Mock SpotOpenRequestRepository spotOpenRequestRepository;
    @Mock SpotImageAdminService spotImageAdminService;
    @Mock SpotImageAccessService spotImageAccessService;
    @Mock KakaoLocalApiClient kakaoLocalApiClient;
    @Mock SpotAlarmService spotAlarmService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock CrowdAreaMapper crowdAreaMapper;
    @Mock SpotRegionResolver spotRegionResolver;

    @InjectMocks MySpotService mySpotService;

    private static final Long USER_ID = 1L;
    private static final Long SPOT_ID = 10L;

    @Nested
    @DisplayName("findMySpots()")
    class FindMySpots {

        @Test
        @DisplayName("위도/경도 모두 null이면 distanceKm 없이 정상 처리된다")
        void successWhenBothNull() {
            given(mySpotMapper.findMySpots(USER_ID, null, null, 0, 6)).willReturn(List.of());
            given(mySpotMapper.countMySpots(USER_ID)).willReturn(0L);

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
            given(mySpotMapper.findMySpots(USER_ID, null, null, 0, 6)).willReturn(List.of(row));
            given(mySpotMapper.countMySpots(USER_ID)).willReturn(1L);
            given(spotImageRepository.findAllBySpotIdIn(List.of(SPOT_ID))).willReturn(List.of(image));
            given(storageService.getUrl("spots/1/image.jpg")).willReturn("https://cdn.example.com/image.jpg");

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, null, null);

            assertThat(response.spots()).hasSize(1);
            assertThat(response.spots().get(0).imageUrl()).isEqualTo("https://cdn.example.com/image.jpg");
            assertThat(response.spots().get(0).bookmarkCount()).isEqualTo(5L);
        }

        @Test
        @DisplayName("이미지가 없는 스팟의 imageUrl은 null이다")
        void imageUrlIsNullWhenNoImage() {
            MySpotRow row = buildRow(SPOT_ID, SpotStatus.PUBLISHED.getCode());
            given(mySpotMapper.findMySpots(USER_ID, null, null, 0, 6)).willReturn(List.of(row));
            given(mySpotMapper.countMySpots(USER_ID)).willReturn(1L);
            given(spotImageRepository.findAllBySpotIdIn(List.of(SPOT_ID))).willReturn(List.of());

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, null, null);

            assertThat(response.spots().get(0).imageUrl()).isNull();
        }

        @Test
        @DisplayName("PENDING 상태 스팟의 status는 enum 이름(PENDING)으로 변환되어 응답된다")
        void includesPendingSpot_withEnumName() {
            MySpotRow row = buildRow(SPOT_ID, SpotStatus.PENDING.getCode());
            given(mySpotMapper.findMySpots(USER_ID, null, null, 0, 6)).willReturn(List.of(row));
            given(mySpotMapper.countMySpots(USER_ID)).willReturn(1L);
            given(spotImageRepository.findAllBySpotIdIn(List.of(SPOT_ID))).willReturn(List.of());

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, null, null);

            assertThat(response.spots().get(0).status()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("PUBLISHED 상태 스팟의 status는 PUBLISHED 이름으로 변환된다")
        void includesPublishedSpot_withEnumName() {
            MySpotRow row = buildRow(SPOT_ID, SpotStatus.PUBLISHED.getCode());
            given(mySpotMapper.findMySpots(USER_ID, null, null, 0, 6)).willReturn(List.of(row));
            given(mySpotMapper.countMySpots(USER_ID)).willReturn(1L);
            given(spotImageRepository.findAllBySpotIdIn(List.of(SPOT_ID))).willReturn(List.of());

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, null, null);

            assertThat(response.spots().get(0).status()).isEqualTo("PUBLISHED");
        }

        @Test
        @DisplayName("REJECTED 상태 스팟의 status는 REJECTED 이름으로 변환된다")
        void includesRejectedSpot_withEnumName() {
            MySpotRow row = buildRow(SPOT_ID, SpotStatus.REJECTED.getCode());
            given(mySpotMapper.findMySpots(USER_ID, null, null, 0, 6)).willReturn(List.of(row));
            given(mySpotMapper.countMySpots(USER_ID)).willReturn(1L);
            given(spotImageRepository.findAllBySpotIdIn(List.of(SPOT_ID))).willReturn(List.of());

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, null, null);

            assertThat(response.spots().get(0).status()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("전체 개수가 다음 페이지 기준 초과 시 hasNext가 true다")
        void hasNextTrue_whenMorePages() {
            given(mySpotMapper.findMySpots(USER_ID, null, null, 0, 6)).willReturn(List.of());
            given(mySpotMapper.countMySpots(USER_ID)).willReturn(7L);

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, null, null);

            assertThat(response.hasNext()).isTrue();
        }

        @Test
        @DisplayName("전체 개수가 다음 페이지 기준 이하면 hasNext가 false다")
        void hasNextFalse_whenLastPage() {
            given(mySpotMapper.findMySpots(USER_ID, null, null, 0, 6)).willReturn(List.of());
            given(mySpotMapper.countMySpots(USER_ID)).willReturn(6L);

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, null, null);

            assertThat(response.hasNext()).isFalse();
        }

        @Test
        @DisplayName("page=1 요청 시 offset=6으로 매퍼에 전달된다")
        void passesCorrectOffset_forSecondPage() {
            given(mySpotMapper.findMySpots(USER_ID, null, null, 6, 6)).willReturn(List.of());
            given(mySpotMapper.countMySpots(USER_ID)).willReturn(0L);

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 1, null, null);

            assertThat(response.page()).isEqualTo(1);
        }
    }

    private MySpotRow buildRow(Long spotId, String statusCode) {
        return new MySpotRow(spotId, "테스트스팟", "SS", 37.5, 127.0, null, LocalDateTime.now(), statusCode, 5L);
    }

    @Nested
    @DisplayName("requestOpen()")
    class RequestOpen {

        @Test
        @DisplayName("DRAFT 스팟을 오픈 신청하면 PENDING으로 전이된다")
        void draftBecomesPending() {
            Spot spot = buildOwnedSpot(SpotStatus.DRAFT);
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(spot));

            var response = mySpotService.requestOpen(USER_ID, SPOT_ID);

            assertThat(response.status()).isEqualTo(SpotStatus.PENDING.name());
            assertThat(spot.getAppliedAt()).isNotNull();
        }

        @Test
        @DisplayName("오픈 신청 시 진행 중 상태의 신청 이력이 한 건 적재된다")
        void savesOpenRequestHistory() {
            Spot spot = buildOwnedSpot(SpotStatus.DRAFT);
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(spot));

            mySpotService.requestOpen(USER_ID, SPOT_ID);

            ArgumentCaptor<SpotOpenRequest> captor = ArgumentCaptor.forClass(SpotOpenRequest.class);
            then(spotOpenRequestRepository).should().save(captor.capture());
            SpotOpenRequest saved = captor.getValue();
            assertThat(saved.getSpotId()).isEqualTo(SPOT_ID);
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getStatus()).isEqualTo(SpotOpenRequestStatus.REQUESTED);
            assertThat(saved.getRequestedAt()).isEqualTo(spot.getAppliedAt());
        }

        @Test
        @DisplayName("반려 후 재신청도 새로운 이력으로 쌓인다")
        void reRequestAlsoSavesHistory() {
            given(spotRepository.findWithLockById(SPOT_ID))
                .willReturn(Optional.of(buildOwnedSpot(SpotStatus.REJECTED)));

            mySpotService.requestOpen(USER_ID, SPOT_ID);

            then(spotOpenRequestRepository).should().save(any(SpotOpenRequest.class));
        }

        @Test
        @DisplayName("신청할 수 없는 상태면 이력을 남기지 않는다")
        void doesNotSaveHistoryWhenNotOpenable() {
            given(spotRepository.findWithLockById(SPOT_ID))
                .willReturn(Optional.of(buildOwnedSpot(SpotStatus.PUBLISHED)));

            assertThatThrownBy(() -> mySpotService.requestOpen(USER_ID, SPOT_ID))
                .isInstanceOf(BusinessException.class);

            then(spotOpenRequestRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("REJECTED 스팟을 재신청하면 RE_REVIEW_PENDING으로 전이된다")
        void rejectedBecomesReReviewPending() {
            Spot spot = buildOwnedSpot(SpotStatus.REJECTED);
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(spot));

            var response = mySpotService.requestOpen(USER_ID, SPOT_ID);

            assertThat(response.status()).isEqualTo(SpotStatus.RE_REVIEW_PENDING.name());
        }

        @Test
        @DisplayName("본인 스팟이 아니면 SPOT_ACCESS_DENIED 예외를 던진다")
        void throwsWhenNotOwner() {
            Spot spot = buildOwnedSpot(SpotStatus.DRAFT);
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(spot));

            assertThatThrownBy(() -> mySpotService.requestOpen(999L, SPOT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(com.ioes.photo.domain.spot.error.SpotErrorCode.SPOT_ACCESS_DENIED);
        }

        @Test
        @DisplayName("운영진 등록 스팟(userId null)이면 NPE 없이 SPOT_ACCESS_DENIED 예외를 던진다")
        void throwsWhenOwnerIsNull() {
            Spot spot = buildSpot(SpotStatus.DRAFT, null);
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(spot));

            assertThatThrownBy(() -> mySpotService.requestOpen(USER_ID, SPOT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(com.ioes.photo.domain.spot.error.SpotErrorCode.SPOT_ACCESS_DENIED);
        }

        @Test
        @DisplayName("오픈 신청 불가 상태(PENDING)면 SPOT_NOT_OPENABLE 예외를 던진다")
        void throwsWhenNotOpenable() {
            Spot spot = buildOwnedSpot(SpotStatus.PENDING);
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(spot));

            assertThatThrownBy(() -> mySpotService.requestOpen(USER_ID, SPOT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(com.ioes.photo.domain.spot.error.SpotErrorCode.SPOT_NOT_OPENABLE);
        }

        @Test
        @DisplayName("존재하지 않는 스팟이면 SPOT_NOT_FOUND 예외를 던진다")
        void throwsWhenNotFound() {
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> mySpotService.requestOpen(USER_ID, SPOT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(com.ioes.photo.domain.spot.error.SpotErrorCode.SPOT_NOT_FOUND);
        }

        private Spot buildOwnedSpot(SpotStatus status) {
            return buildSpot(status, USER_ID);
        }

        private Spot buildSpot(SpotStatus status, Long ownerId) {
            Spot spot = Spot.builder()
                .name("테스트스팟")
                .theme(SpotTheme.SUNSET)
                .latitude(37.5)
                .longitude(127.0)
                .status(status)
                .userId(ownerId)
                .build();
            try {
                Field idField = spot.getClass().getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(spot, SPOT_ID);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            return spot;
        }
    }

    @Nested
    @DisplayName("cancelPublication()")
    class CancelPublication {

        @Test
        @DisplayName("검수중(PENDING) 스팟을 철회하면 DRAFT로 되돌아가고 신청 일시가 비워진다")
        void pendingIsWithdrawnToDraft() {
            Spot spot = ownedSpot(SpotStatus.PENDING);
            spot.requestOpen(LocalDateTime.now());
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.of(spot));

            var response = mySpotService.cancelPublication(USER_ID, SPOT_ID);

            assertThat(response.previousStatus()).isEqualTo(SpotStatus.PENDING.name());
            assertThat(response.status()).isEqualTo(SpotStatus.DRAFT.name());
            assertThat(spot.getStatus()).isEqualTo(SpotStatus.DRAFT);
            assertThat(spot.getAppliedAt()).isNull();
        }

        @Test
        @DisplayName("재검토대기(RE_REVIEW_PENDING) 스팟도 철회할 수 있다")
        void reReviewPendingIsWithdrawnToDraft() {
            given(spotRepository.findWithLockById(SPOT_ID))
                .willReturn(Optional.of(ownedSpot(SpotStatus.RE_REVIEW_PENDING)));

            var response = mySpotService.cancelPublication(USER_ID, SPOT_ID);

            assertThat(response.previousStatus()).isEqualTo(SpotStatus.RE_REVIEW_PENDING.name());
            assertThat(response.status()).isEqualTo(SpotStatus.DRAFT.name());
        }

        @Test
        @DisplayName("철회 시 진행 중이던 신청 이력을 철회 상태로 마감한다")
        void resolvesOpenRequestAsCanceled() {
            SpotOpenRequest openRequest =
                SpotOpenRequest.request(SPOT_ID, USER_ID, LocalDateTime.now().minusDays(1));
            given(spotRepository.findWithLockById(SPOT_ID))
                .willReturn(Optional.of(ownedSpot(SpotStatus.PENDING)));
            given(spotOpenRequestRepository.findFirstBySpotIdAndStatusOrderByRequestedAtDesc(
                SPOT_ID, SpotOpenRequestStatus.REQUESTED)).willReturn(Optional.of(openRequest));

            mySpotService.cancelPublication(USER_ID, SPOT_ID);

            assertThat(openRequest.getStatus()).isEqualTo(SpotOpenRequestStatus.CANCELED);
            assertThat(openRequest.getResolvedAt()).isNotNull();
        }

        @Test
        @DisplayName("공개(PUBLISHED) 스팟을 비공개로 전환하면 이미지도 비공개 경로로 되돌린다")
        void publishedIsUnpublishedWithImages() {
            given(spotRepository.findWithLockById(SPOT_ID))
                .willReturn(Optional.of(ownedSpot(SpotStatus.PUBLISHED)));

            var response = mySpotService.cancelPublication(USER_ID, SPOT_ID);

            assertThat(response.previousStatus()).isEqualTo(SpotStatus.PUBLISHED.name());
            assertThat(response.status()).isEqualTo(SpotStatus.DRAFT.name());
            then(spotImageAccessService).should().unpublish(SPOT_ID);
        }

        @Test
        @DisplayName("철회는 아직 공개된 적이 없으므로 이미지 경로를 건드리지 않는다")
        void withdrawDoesNotTouchImages() {
            given(spotRepository.findWithLockById(SPOT_ID))
                .willReturn(Optional.of(ownedSpot(SpotStatus.PENDING)));

            mySpotService.cancelPublication(USER_ID, SPOT_ID);

            then(spotImageAccessService).should(never()).unpublish(SPOT_ID);
        }

        @Test
        @DisplayName("나만보기(DRAFT) 스팟은 해제할 대상이 없어 SPOT_NOT_CANCELABLE 예외를 던진다")
        void throwsWhenAlreadyDraft() {
            given(spotRepository.findWithLockById(SPOT_ID))
                .willReturn(Optional.of(ownedSpot(SpotStatus.DRAFT)));

            assertThatThrownBy(() -> mySpotService.cancelPublication(USER_ID, SPOT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SpotErrorCode.SPOT_NOT_CANCELABLE);
        }

        @Test
        @DisplayName("철회 직전 운영자가 반려를 확정했다면 SPOT_ALREADY_REVIEWED(409)로 안내한다")
        void throwsAlreadyReviewedWhenRejectedFirst() {
            given(spotRepository.findWithLockById(SPOT_ID))
                .willReturn(Optional.of(ownedSpot(SpotStatus.REJECTED)));

            assertThatThrownBy(() -> mySpotService.cancelPublication(USER_ID, SPOT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("이미 처리된 신청이에요.")
                .extracting("errorCode")
                .isEqualTo(SpotErrorCode.SPOT_ALREADY_REVIEWED);
        }

        @Test
        @DisplayName("본인 스팟이 아니면 SPOT_ACCESS_DENIED 예외를 던진다")
        void throwsWhenNotOwner() {
            given(spotRepository.findWithLockById(SPOT_ID))
                .willReturn(Optional.of(ownedSpot(SpotStatus.PUBLISHED)));

            assertThatThrownBy(() -> mySpotService.cancelPublication(999L, SPOT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SpotErrorCode.SPOT_ACCESS_DENIED);
        }

        @Test
        @DisplayName("존재하지 않는 스팟이면 SPOT_NOT_FOUND 예외를 던진다")
        void throwsWhenNotFound() {
            given(spotRepository.findWithLockById(SPOT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> mySpotService.cancelPublication(USER_ID, SPOT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SpotErrorCode.SPOT_NOT_FOUND);
        }

        private Spot ownedSpot(SpotStatus status) {
            Spot spot = Spot.builder()
                .name("테스트스팟")
                .theme(SpotTheme.SUNSET)
                .latitude(37.5)
                .longitude(127.0)
                .status(status)
                .userId(USER_ID)
                .build();
            ReflectionTestUtils.setField(spot, "id", SPOT_ID);
            return spot;
        }
    }

    @Nested
    @DisplayName("createMySpot()")
    class CreateMySpot {

        private static final String UPLOADED_KEY = "prod/public/spots/10/original/202605/uuid.jpg";

        private CreateMySpotRequest request() {
            return new CreateMySpotRequest(
                "한강 노을 명소",
                SpotTheme.SUNSET,
                37.5326,
                126.9905,
                "코멘트",
                null,
                null
            );
        }

        private MultipartFile image() {
            return new MockMultipartFile("image", "photo.jpg", "image/jpeg", "binary".getBytes());
        }

        private void givenImageUploaded() {
            given(storageProperties.env()).willReturn("prod");
            given(storageService.upload(any(MultipartFile.class), anyString()))
                .willReturn(new UploadResult(UPLOADED_KEY, "photo.jpg", 6L, "image/jpeg"));
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
        @DisplayName("Spot을 DRAFT(나만보기) 상태와 userId로 저장한다")
        void savesSpotAsDraftWithUserId() {
            givenImageUploaded();
            given(spotRepository.save(any(Spot.class))).willAnswer(inv -> {
                Spot s = inv.getArgument(0);
                Field idField = s.getClass().getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(s, SPOT_ID);
                return s;
            });

            mySpotService.createMySpot(USER_ID, request(), image());

            ArgumentCaptor<Spot> captor = ArgumentCaptor.forClass(Spot.class);
            then(spotRepository).should().save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(SpotStatus.DRAFT);
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
            assertThat(captor.getValue().getName()).isEqualTo("한강 노을 명소");
            assertThat(captor.getValue().getTheme()).isEqualTo(SpotTheme.SUNSET);
        }

        @Test
        @DisplayName("서버가 업로드한 이미지 키/파일명/콘텐트타입으로 syncImage를 호출한다")
        void delegatesImageSyncWithUploadedKey() {
            givenImageUploaded();
            given(spotRepository.save(any(Spot.class))).willReturn(savedSpot(SPOT_ID));

            mySpotService.createMySpot(USER_ID, request(), image());

            ArgumentCaptor<SpotImageSyncRequest> reqCaptor = ArgumentCaptor.forClass(SpotImageSyncRequest.class);
            then(spotImageAdminService).should().syncImage(eq(SPOT_ID), reqCaptor.capture());
            assertThat(reqCaptor.getValue().imageKey()).isEqualTo(UPLOADED_KEY);
            assertThat(reqCaptor.getValue().originalFilename()).isEqualTo("photo.jpg");
            assertThat(reqCaptor.getValue().contentType()).isEqualTo("image/jpeg");
        }

        @Test
        @DisplayName("업로드된 이미지 키의 URL을 응답에 매핑한다")
        void mapsUploadedImageUrlIntoResult() {
            givenImageUploaded();
            given(spotRepository.save(any(Spot.class))).willReturn(savedSpot(SPOT_ID));
            given(storageService.getUrl(UPLOADED_KEY)).willReturn("https://cdn/img");

            CreateMySpotResponse response = mySpotService.createMySpot(USER_ID, request(), image());

            assertThat(response.spotId()).isEqualTo(SPOT_ID);
            assertThat(response.status()).isEqualTo(SpotStatus.DRAFT.name());
            assertThat(response.imageUrl()).isEqualTo("https://cdn/img");
        }

        @Test
        @DisplayName("업로드된 이미지 키에 대한 StorageUploadRollbackEvent를 발행한다")
        void publishesUploadRollbackEvent() {
            givenImageUploaded();
            given(spotRepository.save(any(Spot.class))).willReturn(savedSpot(SPOT_ID));

            mySpotService.createMySpot(USER_ID, request(), image());

            then(eventPublisher).should().publishEvent(new StorageUploadRollbackEvent(UPLOADED_KEY));
        }

        @Test
        @DisplayName("격자 좌표/혼잡도 지역을 자동 매핑하고 SpotCreatedEvent를 발행한다")
        void assignsCollectTargetsAndPublishesSpotCreatedEvent() {
            givenImageUploaded();
            given(spotRepository.save(any(Spot.class))).willReturn(savedSpot(SPOT_ID));
            given(crowdAreaMapper.findNearestAreaName(37.5326, 126.9905))
                .willReturn(Optional.of("여의도한강공원"));

            mySpotService.createMySpot(USER_ID, request(), image());

            ArgumentCaptor<Spot> captor = ArgumentCaptor.forClass(Spot.class);
            then(spotRepository).should().save(captor.capture());
            GridPoint expected = LccGridConverter.toGrid(37.5326, 126.9905);
            assertThat(captor.getValue().getGridNx()).isEqualTo(expected.nx());
            assertThat(captor.getValue().getGridNy()).isEqualTo(expected.ny());
            assertThat(captor.getValue().getCrowdAreaName()).isEqualTo("여의도한강공원");
            then(eventPublisher).should().publishEvent(new SpotCreatedEvent(SPOT_ID));
        }

        @Test
        @DisplayName("등록한 스팟에 대한 촬영조건 알림 구독을 적재한다")
        void subscribesAlarmForCreatedSpot() {
            givenImageUploaded();
            given(spotRepository.save(any(Spot.class))).willReturn(savedSpot(SPOT_ID));

            mySpotService.createMySpot(USER_ID, request(), image());

            then(spotAlarmService).should().subscribe(USER_ID, SPOT_ID);
        }

        @Test
        @DisplayName("역지오코딩 결과를 도로명/지번/시구 주소로 저장한다")
        void savesReverseGeocodedAddress() {
            givenImageUploaded();
            given(kakaoLocalApiClient.reverseGeocode(37.5326, 126.9905))
                .willReturn(Optional.of(new KakaoAddress(
                    "서울특별시 영등포구 여의공원로 68",
                    "서울특별시 영등포구 여의도동 18",
                    "서울특별시 영등포구")));
            given(spotRepository.save(any(Spot.class))).willReturn(savedSpot(SPOT_ID));

            mySpotService.createMySpot(USER_ID, request(), image());

            ArgumentCaptor<Spot> captor = ArgumentCaptor.forClass(Spot.class);
            then(spotRepository).should().save(captor.capture());
            assertThat(captor.getValue().getAddress()).isEqualTo("서울특별시 영등포구");
            assertThat(captor.getValue().getAddressRoad()).isEqualTo("서울특별시 영등포구 여의공원로 68");
            assertThat(captor.getValue().getAddressJibun()).isEqualTo("서울특별시 영등포구 여의도동 18");
        }

        @Test
        @DisplayName("역지오코딩 실패 시 주소 없이 스팟을 등록한다")
        void savesSpotWithoutAddress_whenReverseGeocodeFails() {
            givenImageUploaded();
            given(kakaoLocalApiClient.reverseGeocode(37.5326, 126.9905))
                .willThrow(new BusinessException(ExternalApiErrorCode.API_CALL_FAILED));
            given(spotRepository.save(any(Spot.class))).willReturn(savedSpot(SPOT_ID));

            CreateMySpotResponse response = mySpotService.createMySpot(USER_ID, request(), image());

            assertThat(response.spotId()).isEqualTo(SPOT_ID);
            ArgumentCaptor<Spot> captor = ArgumentCaptor.forClass(Spot.class);
            then(spotRepository).should().save(captor.capture());
            assertThat(captor.getValue().getAddress()).isNull();
            assertThat(captor.getValue().getAddressRoad()).isNull();
            assertThat(captor.getValue().getAddressJibun()).isNull();
        }
    }
}
