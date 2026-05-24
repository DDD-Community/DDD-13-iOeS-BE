package com.ioes.photo.domain.spot.service;

import com.ioes.photo.domain.spot.dto.SpotDetailResponse;
import com.ioes.photo.domain.spot.dto.SpotListResponse;
import com.ioes.photo.domain.spot.dto.SpotListResponse.SpotItem;
import com.ioes.photo.domain.spot.dto.SpotPreviewResponse;
import com.ioes.photo.domain.spot.dto.SpotViewportResponse;
import com.ioes.photo.domain.spot.dto.SpotViewportResponse.SpotSummary;
import com.ioes.photo.domain.spot.dto.ViewportRequest;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.enums.SortType;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.error.SpotErrorCode;
import com.ioes.photo.domain.spot.mapper.SpotMapper;
import com.ioes.photo.domain.spot.mapper.SpotPreviewRow;
import com.ioes.photo.domain.spot.mapper.SpotRow;
import com.ioes.photo.domain.spot.mapper.SpotViewportRow;
import com.ioes.photo.domain.savedspot.entity.SavedSpotArchive;
import com.ioes.photo.domain.savedspot.repository.SavedSpotArchiveRepository;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spotinfo.entity.SpotInfo;
import com.ioes.photo.domain.spotinfo.repository.SpotInfoRepository;
import com.ioes.photo.domain.user.repository.UserRepository;
import com.ioes.photo.external.crowd.enums.CongestionLevel;
import com.ioes.photo.external.weather.enums.PrecipitationType;
import com.ioes.photo.external.weather.enums.SkyStatus;
import com.ioes.photo.global.error.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * {@link SpotQueryService} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SpotQueryService 단위 테스트")
class SpotQueryServiceTest {

    @Mock SpotRepository      spotRepository;
    @Mock SpotImageRepository spotImageRepository;
    @Mock SpotInfoRepository  spotInfoRepository;
    @Mock SavedSpotArchiveRepository savedSpotArchiveRepository;
    @Mock SpotThumbnailService spotThumbnailService;
    @Mock SpotMapper          spotMapper;
    @Mock UserRepository      userRepository;

    @InjectMocks SpotQueryService spotQueryService;

    private static final String PUBLISHED_CODE = SpotStatus.PUBLISHED.getCode();

    @BeforeEach
    void setUpUserRepository() {
        // 기본적으로 모든 업로더가 활성 상태라고 가정 (탈퇴 유저 처리 테스트 제외)
        given(userRepository.findActiveIdsByIdIn(any())).willAnswer(inv -> inv.getArgument(0));
    }

    // ── findSpotsInViewport ──────────────────────────────────────────────────

    @Nested
    @DisplayName("findSpotsInViewport()")
    class FindSpotsInViewport {

        private final ViewportRequest viewport =
            new ViewportRequest(37.6, 127.0, 37.6, 127.1, 37.5, 127.0, 37.5, 127.1);

        @Test
        @DisplayName("뷰포트 내 스팟이 있으면 thumbnailUrl을 포함한 SpotSummary 목록을 반환한다")
        void returnsSummariesWithThumbnail_whenSpotsFound() {
            SpotViewportRow row = new SpotViewportRow(1L, 37.55, 127.05, null);
            SpotImage image = SpotImage.create(1L, "prod/public/spots/1/thumbnail/key.jpg");

            given(spotMapper.findSpotsInViewport(37.5, 37.6, 127.0, 127.1, PUBLISHED_CODE, null))
                .willReturn(List.of(row));
            given(spotImageRepository.findAllBySpotIdIn(List.of(1L)))
                .willReturn(List.of(image));
            given(spotThumbnailService.getThumbnailUrl(image))
                .willReturn("https://cdn.example.com/thumbnail.jpg");

            SpotViewportResponse response = spotQueryService.findSpotsInViewport(viewport, null, null);

            assertThat(response.spots()).hasSize(1);
            SpotSummary summary = response.spots().get(0);
            assertThat(summary.spotId()).isEqualTo(1L);
            assertThat(summary.spotImageUrl()).isEqualTo("https://cdn.example.com/thumbnail.jpg");
            assertThat(summary.latitude()).isEqualTo(37.55);
            assertThat(summary.longitude()).isEqualTo(127.05);
            assertThat(summary.isMySpot()).isFalse();
        }

        @Test
        @DisplayName("로그인 사용자의 스팟이면 isMySpot이 true다")
        void isMySpotTrue_whenSpotBelongsToUser() {
            SpotViewportRow row = new SpotViewportRow(1L, 37.55, 127.05, 42L);

            given(spotMapper.findSpotsInViewport(37.5, 37.6, 127.0, 127.1, PUBLISHED_CODE, null))
                .willReturn(List.of(row));
            given(spotImageRepository.findAllBySpotIdIn(List.of(1L)))
                .willReturn(List.of());

            SpotViewportResponse response = spotQueryService.findSpotsInViewport(viewport, null, 42L);

            assertThat(response.spots().get(0).isMySpot()).isTrue();
        }

        @Test
        @DisplayName("다른 사용자의 스팟이면 isMySpot이 false다")
        void isMySpotFalse_whenSpotBelongsToDifferentUser() {
            SpotViewportRow row = new SpotViewportRow(1L, 37.55, 127.05, 99L);

            given(spotMapper.findSpotsInViewport(37.5, 37.6, 127.0, 127.1, PUBLISHED_CODE, null))
                .willReturn(List.of(row));
            given(spotImageRepository.findAllBySpotIdIn(List.of(1L)))
                .willReturn(List.of());

            SpotViewportResponse response = spotQueryService.findSpotsInViewport(viewport, null, 42L);

            assertThat(response.spots().get(0).isMySpot()).isFalse();
        }

        @Test
        @DisplayName("이미지가 없는 스팟의 thumbnailUrl은 null이다")
        void returnsNullThumbnail_whenNoImage() {
            SpotViewportRow row = new SpotViewportRow(2L, 37.55, 127.05, null);

            given(spotMapper.findSpotsInViewport(37.5, 37.6, 127.0, 127.1, PUBLISHED_CODE, null))
                .willReturn(List.of(row));
            given(spotImageRepository.findAllBySpotIdIn(List.of(2L)))
                .willReturn(List.of());

            SpotViewportResponse response = spotQueryService.findSpotsInViewport(viewport, null, null);

            assertThat(response.spots()).hasSize(1);
            assertThat(response.spots().get(0).spotImageUrl()).isNull();
        }

        @Test
        @DisplayName("뷰포트 내 스팟이 없으면 빈 목록을 반환하고 이미지 조회는 수행하지 않는다")
        void returnsEmpty_whenNoSpotsFound() {
            given(spotMapper.findSpotsInViewport(37.5, 37.6, 127.0, 127.1, PUBLISHED_CODE, null))
                .willReturn(List.of());

            SpotViewportResponse response = spotQueryService.findSpotsInViewport(viewport, null, null);

            assertThat(response.spots()).isEmpty();
            then(spotImageRepository).should(never()).findAllBySpotIdIn(anyList());
        }

        @Test
        @DisplayName("theme 필터를 mapper에 코드값으로 전달한다")
        void passesThemeCodeToMapper() {
            given(spotMapper.findSpotsInViewport(37.5, 37.6, 127.0, 127.1, PUBLISHED_CODE, SpotTheme.SUNSET.getCode()))
                .willReturn(List.of());

            spotQueryService.findSpotsInViewport(viewport, SpotTheme.SUNSET, null);

            then(spotMapper).should()
                .findSpotsInViewport(37.5, 37.6, 127.0, 127.1, PUBLISHED_CODE, SpotTheme.SUNSET.getCode());
        }
    }

    // ── findSpotPreview ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("findSpotPreview()")
    class FindSpotPreview {

        @Test
        @DisplayName("스팟이 존재하면 미리보기 응답을 반환한다")
        void returnsPreview_whenSpotExists() {
            SpotPreviewRow row = new SpotPreviewRow(1L, "한강공원", "SS", null, 5L, 1.2,
                "서울시 마포구", "서울시 마포구 월드컵로 21", "서울시 마포구 망원동 1");
            SpotImage image = SpotImage.create(1L, "prod/public/spots/1/thumbnail/key.jpg");
            given(spotMapper.findSpotPreview(1L, 37.5, 127.0)).willReturn(row);
            given(spotImageRepository.findById(1L)).willReturn(Optional.of(image));
            given(spotThumbnailService.getThumbnailUrl(image)).willReturn("https://cdn.example.com/thumbnail.jpg");

            SpotPreviewResponse response = spotQueryService.findSpotPreview(1L, 37.5, 127.0, null);

            assertThat(response.spotId()).isEqualTo(1L);
            assertThat(response.name()).isEqualTo("한강공원");
            assertThat(response.isMySpot()).isFalse();
            assertThat(response.theme()).isEqualTo(SpotTheme.SUNSET);
            assertThat(response.bookmarkCount()).isEqualTo(5L);
            assertThat(response.distanceKm()).isEqualTo(1.2);
            assertThat(response.imageUrl()).isEqualTo("https://cdn.example.com/thumbnail.jpg");
            assertThat(response.addressSimple()).isEqualTo("서울시 마포구");
            assertThat(response.addressRoad()).isEqualTo("서울시 마포구 월드컵로 21");
            assertThat(response.addressJibun()).isEqualTo("서울시 마포구 망원동 1");
        }

        @Test
        @DisplayName("이미지가 없는 스팟의 imageUrl은 null이다")
        void imageUrlNull_whenNoImage() {
            SpotPreviewRow row = new SpotPreviewRow(1L, "한강공원", "SS", null, 5L, null, "서울시 마포구", null, null);
            given(spotMapper.findSpotPreview(1L, null, null)).willReturn(row);
            given(spotImageRepository.findById(1L)).willReturn(Optional.empty());

            SpotPreviewResponse response = spotQueryService.findSpotPreview(1L, null, null, null);

            assertThat(response.imageUrl()).isNull();
        }

        @Test
        @DisplayName("스팟이 존재하지 않으면 SPOT_NOT_FOUND 예외를 던진다")
        void throwsSpotNotFound_whenRowNull() {
            given(spotMapper.findSpotPreview(99L, null, null)).willReturn(null);

            assertThatThrownBy(() -> spotQueryService.findSpotPreview(99L, null, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SpotErrorCode.SPOT_NOT_FOUND);
        }

        @Test
        @DisplayName("로그인 사용자가 스팟 등록자면 isMySpot이 true다")
        void isMySpotTrue_whenUserIsOwner() {
            SpotPreviewRow row = new SpotPreviewRow(1L, "한강공원", "SS", 42L, 5L, null, "서울시 마포구", null, null);
            given(spotMapper.findSpotPreview(1L, null, null)).willReturn(row);
            given(spotImageRepository.findById(1L)).willReturn(Optional.empty());

            SpotPreviewResponse response = spotQueryService.findSpotPreview(1L, null, null, 42L);

            assertThat(response.isMySpot()).isTrue();
        }

        @Test
        @DisplayName("다른 사용자면 isMySpot이 false다")
        void isMySpotFalse_whenDifferentUser() {
            SpotPreviewRow row = new SpotPreviewRow(1L, "한강공원", "SS", 99L, 5L, null, "서울시 마포구", null, null);
            given(spotMapper.findSpotPreview(1L, null, null)).willReturn(row);
            given(spotImageRepository.findById(1L)).willReturn(Optional.empty());

            SpotPreviewResponse response = spotQueryService.findSpotPreview(1L, null, null, 42L);

            assertThat(response.isMySpot()).isFalse();
        }

        @Test
        @DisplayName("위도/경도 미제공 시 distanceKm는 null이다")
        void distanceKmNull_whenNoCoordinates() {
            SpotPreviewRow row = new SpotPreviewRow(1L, "한강공원", "SS", null, 5L, null, "서울시 마포구", null, null);
            given(spotMapper.findSpotPreview(1L, null, null)).willReturn(row);
            given(spotImageRepository.findById(1L)).willReturn(Optional.empty());

            SpotPreviewResponse response = spotQueryService.findSpotPreview(1L, null, null, null);

            assertThat(response.distanceKm()).isNull();
        }

        @Test
        @DisplayName("userId가 null이면 isMySpot은 false다")
        void isMySpotFalse_whenUserIdNull() {
            SpotPreviewRow row = new SpotPreviewRow(1L, "한강공원", "SS", 42L, 5L, 1.2, "서울시 마포구", null, null);
            given(spotMapper.findSpotPreview(1L, null, null)).willReturn(row);
            given(spotImageRepository.findById(1L)).willReturn(Optional.empty());

            SpotPreviewResponse response = spotQueryService.findSpotPreview(1L, null, null, null);

            assertThat(response.isMySpot()).isFalse();
        }
    }

    // ── findSpots ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findSpots()")
    class FindSpots {

        @Test
        @DisplayName("위도만 전달하면 INVALID_INPUT_VALUE 예외를 던진다")
        void throwsWhenOnlyLatProvided() {
            assertThatThrownBy(() -> spotQueryService.findSpots(0, null, 37.5, null, SortType.RECOMMENDED))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("경도만 전달하면 INVALID_INPUT_VALUE 예외를 던진다")
        void throwsWhenOnlyLngProvided() {
            assertThatThrownBy(() -> spotQueryService.findSpots(0, null, null, 127.0, SortType.RECOMMENDED))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("DISTANCE 정렬에 위치 없으면 INVALID_INPUT_VALUE 예외를 던진다")
        void throwsWhenDistanceSortWithoutCoordinates() {
            assertThatThrownBy(() -> spotQueryService.findSpots(0, null, null, null, SortType.DISTANCE))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("위도/경도 모두 null이면 정상 처리된다")
        void successWhenBothCoordinatesNull() {
            given(spotMapper.findSpots(PUBLISHED_CODE, null, null, null, 0, 6, SortType.RECOMMENDED.getCode()))
                .willReturn(List.of());
            given(spotMapper.countSpots(PUBLISHED_CODE, null)).willReturn(0L);

            SpotListResponse response = spotQueryService.findSpots(0, null, null, null, SortType.RECOMMENDED);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("위도/경도 모두 있고 DISTANCE 정렬이면 mapper에 전달한다")
        void passesLatLngToMapperWhenDistanceSort() {
            given(spotMapper.findSpots(PUBLISHED_CODE, null, 37.5, 127.0, 0, 6, SortType.DISTANCE.getCode()))
                .willReturn(List.of());
            given(spotMapper.countSpots(PUBLISHED_CODE, null)).willReturn(0L);

            spotQueryService.findSpots(0, null, 37.5, 127.0, SortType.DISTANCE);

            then(spotMapper).should().findSpots(PUBLISHED_CODE, null, 37.5, 127.0, 0, 6, SortType.DISTANCE.getCode());
        }

        @Test
        @DisplayName("테마 필터가 있으면 mapper에 테마 코드를 전달한다")
        void passesThemeCodeToMapper() {
            given(spotMapper.findSpots(PUBLISHED_CODE, SpotTheme.SUNSET.getCode(), null, null, 0, 6, SortType.RECOMMENDED.getCode()))
                .willReturn(List.of());
            given(spotMapper.countSpots(PUBLISHED_CODE, SpotTheme.SUNSET.getCode())).willReturn(0L);

            spotQueryService.findSpots(0, SpotTheme.SUNSET, null, null, SortType.RECOMMENDED);

            then(spotMapper).should().findSpots(PUBLISHED_CODE, SpotTheme.SUNSET.getCode(), null, null, 0, 6, SortType.RECOMMENDED.getCode());
        }

        @Test
        @DisplayName("전체 개수가 다음 페이지 기준을 초과하면 hasNext가 true다")
        void hasNextTrue_whenMorePagesExist() {
            List<SpotRow> rows = List.of(
                new SpotRow(1L, "스팟A", "SS", null),
                new SpotRow(2L, "스팟B", "SS", null),
                new SpotRow(3L, "스팟C", "SS", null),
                new SpotRow(4L, "스팟D", "SS", null),
                new SpotRow(5L, "스팟E", "SS", null),
                new SpotRow(6L, "스팟F", "SS", null)
            );
            given(spotMapper.findSpots(PUBLISHED_CODE, null, null, null, 0, 6, SortType.RECOMMENDED.getCode())).willReturn(rows);
            given(spotMapper.countSpots(PUBLISHED_CODE, null)).willReturn(7L);
            given(spotImageRepository.findAllBySpotIdIn(any())).willReturn(List.of());

            SpotListResponse response = spotQueryService.findSpots(0, null, null, null, SortType.RECOMMENDED);

            assertThat(response.hasNext()).isTrue();
        }

        @Test
        @DisplayName("전체 개수가 다음 페이지 기준 이하면 hasNext가 false다")
        void hasNextFalse_whenLastPage() {
            given(spotMapper.findSpots(PUBLISHED_CODE, null, null, null, 0, 6, SortType.RECOMMENDED.getCode())).willReturn(List.of());
            given(spotMapper.countSpots(PUBLISHED_CODE, null)).willReturn(6L);

            SpotListResponse response = spotQueryService.findSpots(0, null, null, null, SortType.RECOMMENDED);

            assertThat(response.hasNext()).isFalse();
        }

        @Test
        @DisplayName("page=1이면 offset=6으로 mapper를 호출한다")
        void passesCorrectOffsetForPage1() {
            given(spotMapper.findSpots(PUBLISHED_CODE, null, null, null, 6, 6, SortType.RECOMMENDED.getCode())).willReturn(List.of());
            given(spotMapper.countSpots(PUBLISHED_CODE, null)).willReturn(0L);

            spotQueryService.findSpots(1, null, null, null, SortType.RECOMMENDED);

            then(spotMapper).should().findSpots(PUBLISHED_CODE, null, null, null, 6, 6, SortType.RECOMMENDED.getCode());
        }

        @Test
        @DisplayName("응답에 요청한 page 번호가 그대로 포함된다")
        void responseContainsRequestedPage() {
            given(spotMapper.findSpots(PUBLISHED_CODE, null, null, null, 12, 6, SortType.RECOMMENDED.getCode())).willReturn(List.of());
            given(spotMapper.countSpots(PUBLISHED_CODE, null)).willReturn(0L);

            SpotListResponse response = spotQueryService.findSpots(2, null, null, null, SortType.RECOMMENDED);

            assertThat(response.page()).isEqualTo(2);
        }

        @Test
        @DisplayName("SpotRow의 모든 필드가 SpotItem에 올바르게 매핑된다")
        void mapsSpotRowToSpotItemCorrectly() {
            SpotImage image = SpotImage.create(1L, "key.jpg");
            SpotRow row = new SpotRow(1L, "한강공원", "SS", 1.5);
            given(spotMapper.findSpots(PUBLISHED_CODE, null, null, null, 0, 6, SortType.RECOMMENDED.getCode())).willReturn(List.of(row));
            given(spotMapper.countSpots(PUBLISHED_CODE, null)).willReturn(1L);
            given(spotImageRepository.findAllBySpotIdIn(List.of(1L))).willReturn(List.of(image));
            given(spotThumbnailService.getThumbnailUrl(image)).willReturn("https://cdn.example.com/thumb.jpg");

            SpotListResponse response = spotQueryService.findSpots(0, null, null, null, SortType.RECOMMENDED);

            assertThat(response.spots()).hasSize(1);
            SpotItem item = response.spots().get(0);
            assertThat(item.spotId()).isEqualTo(1L);
            assertThat(item.name()).isEqualTo("한강공원");
            assertThat(item.theme()).isEqualTo("SS");
            assertThat(item.thumbnailUrl()).isEqualTo("https://cdn.example.com/thumb.jpg");
            assertThat(item.distanceKm()).isEqualTo(1.5);
        }

        @Test
        @DisplayName("SpotRow 결과가 비어있으면 이미지 조회를 수행하지 않는다")
        void skipsImageQuery_whenNoRows() {
            given(spotMapper.findSpots(PUBLISHED_CODE, null, null, null, 0, 6, SortType.RECOMMENDED.getCode())).willReturn(List.of());
            given(spotMapper.countSpots(PUBLISHED_CODE, null)).willReturn(0L);

            spotQueryService.findSpots(0, null, null, null, SortType.RECOMMENDED);

            then(spotImageRepository).should(never()).findAllBySpotIdIn(anyList());
        }
    }

    // ── findSpotDetail ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("findSpotDetail()")
    class FindSpotDetail {

        @Test
        @DisplayName("Spot이 존재하면 이미지/날씨/혼잡도/일몰/북마크/MySpot 정보를 포함한 상세 응답을 반환한다")
        void returnsDetailWhenSpotExists() {
            Spot spot = buildSpot(1L, 37.55, 127.05);
            ReflectionTestUtils.setField(spot, "comment", "노을이 예쁜 곳");
            ReflectionTestUtils.setField(spot, "address", "서울시 마포구");
            ReflectionTestUtils.setField(spot, "bookmarkCount", 5L);
            ReflectionTestUtils.setField(spot, "userId", 42L);

            SpotImage image = SpotImage.create(1L, "spots/1/original.jpg");
            image.updateRecordedDate(java.time.LocalDate.of(2025, 5, 1));
            image.updateRecordedTime(LocalTime.of(18, 30));

            SpotInfo info = SpotInfo.create(1L);
            info.updateWeather(SkyStatus.CLEAR, PrecipitationType.NONE, 20, 23.5,
                java.time.LocalDateTime.of(2025, 5, 1, 17, 0));
            info.updateCrowd(CongestionLevel.NORMAL, "보통", 1000, 2000,
                java.time.LocalDateTime.of(2025, 5, 1, 17, 5));
            info.updateAstronomy(java.time.LocalDate.of(2025, 5, 1), LocalTime.of(5, 45), LocalTime.of(18, 55));

            given(spotRepository.findById(1L)).willReturn(Optional.of(spot));
            given(spotImageRepository.findById(1L)).willReturn(Optional.of(image));
            given(spotInfoRepository.findById(1L)).willReturn(Optional.of(info));
            given(spotThumbnailService.getImageUrl(image)).willReturn("https://cdn.example.com/original.jpg");
            given(savedSpotArchiveRepository.findByUserIdAndSpotId(42L, 1L))
                .willReturn(Optional.of(SavedSpotArchive.builder().userId(42L).spotId(1L).build()));

            SpotDetailResponse response = spotQueryService.findSpotDetail(1L, 42L);

            assertThat(response.spotId()).isEqualTo(1L);
            assertThat(response.name()).isEqualTo("테스트스팟");
            assertThat(response.comment()).isEqualTo("노을이 예쁜 곳");
            assertThat(response.theme()).isEqualTo(SpotTheme.SUNSET);
            assertThat(response.latitude()).isEqualTo(37.55);
            assertThat(response.longitude()).isEqualTo(127.05);
            assertThat(response.address()).isEqualTo("서울시 마포구");
            assertThat(response.imageUrl()).isEqualTo("https://cdn.example.com/original.jpg");
            assertThat(response.recordedDate()).isEqualTo(java.time.LocalDate.of(2025, 5, 1));
            assertThat(response.recordedTime()).isEqualTo(LocalTime.of(18, 30));
            assertThat(response.weatherSky()).isEqualTo(SkyStatus.CLEAR);
            assertThat(response.precipitation()).isEqualTo(PrecipitationType.NONE);
            assertThat(response.precipitationProbability()).isEqualTo(20);
            assertThat(response.congestionLevel()).isEqualTo(CongestionLevel.NORMAL);
            assertThat(response.sunsetTime()).isEqualTo(LocalTime.of(18, 55));
            assertThat(response.astronomyDate()).isEqualTo(java.time.LocalDate.of(2025, 5, 1));
            assertThat(response.weatherUpdatedAt()).isEqualTo(java.time.LocalDateTime.of(2025, 5, 1, 17, 0));
            assertThat(response.congestionUpdatedAt()).isEqualTo(java.time.LocalDateTime.of(2025, 5, 1, 17, 5));
            assertThat(response.parkingInfo()).isEqualTo("정보 없음");
            assertThat(response.bookmarkCount()).isEqualTo(5L);
            assertThat(response.isBookmarked()).isTrue();
            assertThat(response.isMySpot()).isTrue();
        }

        @Test
        @DisplayName("Spot이 존재하지 않으면 SPOT_NOT_FOUND BusinessException을 던진다")
        void throwsSpotNotFound_whenSpotMissing() {
            given(spotRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> spotQueryService.findSpotDetail(99L, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SpotErrorCode.SPOT_NOT_FOUND);
        }

        @Test
        @DisplayName("SpotImage가 없으면 imageUrl/recordedDate/recordedTime은 null이다")
        void nullsImageFields_whenSpotImageMissing() {
            Spot spot = buildSpot(1L, 37.55, 127.05);
            given(spotRepository.findById(1L)).willReturn(Optional.of(spot));
            given(spotImageRepository.findById(1L)).willReturn(Optional.empty());
            given(spotInfoRepository.findById(1L)).willReturn(Optional.empty());

            SpotDetailResponse response = spotQueryService.findSpotDetail(1L, null);

            assertThat(response.imageUrl()).isNull();
            assertThat(response.recordedDate()).isNull();
            assertThat(response.recordedTime()).isNull();
        }

        @Test
        @DisplayName("SpotInfo가 없으면 날씨/혼잡도/일몰 필드는 모두 null이다")
        void nullsSpotInfoFields_whenInfoMissing() {
            Spot spot = buildSpot(1L, 37.55, 127.05);
            SpotImage image = SpotImage.create(1L, "spots/1/original.jpg");

            given(spotRepository.findById(1L)).willReturn(Optional.of(spot));
            given(spotImageRepository.findById(1L)).willReturn(Optional.of(image));
            given(spotInfoRepository.findById(1L)).willReturn(Optional.empty());
            given(spotThumbnailService.getImageUrl(image)).willReturn("https://cdn.example.com/original.jpg");

            SpotDetailResponse response = spotQueryService.findSpotDetail(1L, null);

            assertThat(response.weatherSky()).isNull();
            assertThat(response.precipitation()).isNull();
            assertThat(response.precipitationProbability()).isNull();
            assertThat(response.congestionLevel()).isNull();
            assertThat(response.sunsetTime()).isNull();
            assertThat(response.astronomyDate()).isNull();
            assertThat(response.weatherUpdatedAt()).isNull();
            assertThat(response.congestionUpdatedAt()).isNull();
        }

        @Test
        @DisplayName("userId가 null이면 isBookmarked/isMySpot은 false이고 북마크 조회를 수행하지 않는다")
        void noBookmarkLookup_whenUserIdNull() {
            Spot spot = buildSpot(1L, 37.55, 127.05);
            given(spotRepository.findById(1L)).willReturn(Optional.of(spot));
            given(spotImageRepository.findById(1L)).willReturn(Optional.empty());
            given(spotInfoRepository.findById(1L)).willReturn(Optional.empty());

            SpotDetailResponse response = spotQueryService.findSpotDetail(1L, null);

            assertThat(response.isBookmarked()).isFalse();
            assertThat(response.isMySpot()).isFalse();
            then(savedSpotArchiveRepository).should(never())
                .findByUserIdAndSpotId(any(Long.class), any(Long.class));
        }

        @Test
        @DisplayName("북마크 기록이 없으면 isBookmarked가 false다")
        void isBookmarkedFalse_whenNoBookmarkExists() {
            Spot spot = buildSpot(1L, 37.55, 127.05);
            given(spotRepository.findById(1L)).willReturn(Optional.of(spot));
            given(spotImageRepository.findById(1L)).willReturn(Optional.empty());
            given(spotInfoRepository.findById(1L)).willReturn(Optional.empty());
            given(savedSpotArchiveRepository.findByUserIdAndSpotId(7L, 1L)).willReturn(Optional.empty());

            SpotDetailResponse response = spotQueryService.findSpotDetail(1L, 7L);

            assertThat(response.isBookmarked()).isFalse();
        }

        @Test
        @DisplayName("Spot의 userId와 다르면 isMySpot이 false다")
        void isMySpotFalse_whenUserIdDoesNotMatch() {
            Spot spot = buildSpot(1L, 37.55, 127.05);
            ReflectionTestUtils.setField(spot, "userId", 99L);
            given(spotRepository.findById(1L)).willReturn(Optional.of(spot));
            given(spotImageRepository.findById(1L)).willReturn(Optional.empty());
            given(spotInfoRepository.findById(1L)).willReturn(Optional.empty());
            given(savedSpotArchiveRepository.findByUserIdAndSpotId(7L, 1L)).willReturn(Optional.empty());

            SpotDetailResponse response = spotQueryService.findSpotDetail(1L, 7L);

            assertThat(response.isMySpot()).isFalse();
        }

        @Test
        @DisplayName("PENDING/REJECTED 상태의 Spot도 조회된다")
        void returnsNonPublishedSpot() {
            Spot pending = Spot.builder()
                .name("승인대기")
                .theme(SpotTheme.YUNSEUL)
                .latitude(37.5)
                .longitude(127.0)
                .status(SpotStatus.PENDING)
                .build();
            ReflectionTestUtils.setField(pending, "id", 7L);

            given(spotRepository.findById(7L)).willReturn(Optional.of(pending));
            given(spotImageRepository.findById(7L)).willReturn(Optional.empty());
            given(spotInfoRepository.findById(7L)).willReturn(Optional.empty());

            SpotDetailResponse response = spotQueryService.findSpotDetail(7L, null);

            assertThat(response.spotId()).isEqualTo(7L);
            assertThat(response.name()).isEqualTo("승인대기");
        }
    }

    // ── helper ──────────────────────────────────────────────────────────────

    private static Spot buildSpot(Long id, double lat, double lng) {
        Spot spot = Spot.builder()
            .name("테스트스팟")
            .theme(SpotTheme.SUNSET)
            .latitude(lat)
            .longitude(lng)
            .status(SpotStatus.PUBLISHED)
            .build();
        ReflectionTestUtils.setField(spot, "id", id);
        return spot;
    }
}
