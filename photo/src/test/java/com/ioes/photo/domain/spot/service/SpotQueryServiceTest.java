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
import com.ioes.photo.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

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
@DisplayName("SpotQueryService 단위 테스트")
class SpotQueryServiceTest {

    @Mock SpotRepository      spotRepository;
    @Mock SpotImageRepository spotImageRepository;
    @Mock SpotThumbnailService spotThumbnailService;
    @Mock SpotMapper          spotMapper;

    @InjectMocks SpotQueryService spotQueryService;

    private static final String PUBLISHED_CODE = SpotStatus.PUBLISHED.getCode();
    private static final String PUBLISHED_NAME = SpotStatus.PUBLISHED.name();

    // ── findSpotsInViewport ──────────────────────────────────────────────────

    @Nested
    @DisplayName("findSpotsInViewport()")
    class FindSpotsInViewport {

        private final ViewportRequest viewport =
            new ViewportRequest(37.6, 127.0, 37.6, 127.1, 37.5, 127.0, 37.5, 127.1);

        @Test
        @DisplayName("뷰포트 내 스팟이 있으면 thumbnailUrl을 포함한 SpotSummary 목록을 반환한다")
        void returnsSummariesWithThumbnail_whenSpotsFound() {
            Spot spot = buildSpot(1L, 37.55, 127.05);
            SpotImage image = SpotImage.create(1L, "prod/public/spots/1/thumbnail/key.jpg");

            given(spotRepository.findAllInViewport(37.5, 37.6, 127.0, 127.1, PUBLISHED_CODE))
                .willReturn(List.of(spot));
            given(spotImageRepository.findAllBySpotIdIn(List.of(1L)))
                .willReturn(List.of(image));
            given(spotThumbnailService.getThumbnailUrl(image))
                .willReturn("https://cdn.example.com/thumbnail.jpg");

            SpotViewportResponse response = spotQueryService.findSpotsInViewport(viewport);

            assertThat(response.spots()).hasSize(1);
            SpotSummary summary = response.spots().get(0);
            assertThat(summary.spotId()).isEqualTo(1L);
            assertThat(summary.spotImageUrl()).isEqualTo("https://cdn.example.com/thumbnail.jpg");
            assertThat(summary.latitude()).isEqualTo(37.55);
            assertThat(summary.longitude()).isEqualTo(127.05);
        }

        @Test
        @DisplayName("이미지가 없는 스팟의 thumbnailUrl은 null이다")
        void returnsNullThumbnail_whenNoImage() {
            Spot spot = buildSpot(2L, 37.55, 127.05);

            given(spotRepository.findAllInViewport(37.5, 37.6, 127.0, 127.1, PUBLISHED_CODE))
                .willReturn(List.of(spot));
            given(spotImageRepository.findAllBySpotIdIn(List.of(2L)))
                .willReturn(List.of());

            SpotViewportResponse response = spotQueryService.findSpotsInViewport(viewport);

            assertThat(response.spots()).hasSize(1);
            assertThat(response.spots().get(0).spotImageUrl()).isNull();
        }

        @Test
        @DisplayName("뷰포트 내 스팟이 없으면 빈 목록을 반환하고 이미지 조회는 수행하지 않는다")
        void returnsEmpty_whenNoSpotsFound() {
            given(spotRepository.findAllInViewport(37.5, 37.6, 127.0, 127.1, PUBLISHED_CODE))
                .willReturn(List.of());

            SpotViewportResponse response = spotQueryService.findSpotsInViewport(viewport);

            assertThat(response.spots()).isEmpty();
            then(spotImageRepository).should(never()).findAllBySpotIdIn(anyList());
        }

        @Test
        @DisplayName("repository에 올바른 min/max 좌표와 PUBLISHED 코드를 전달한다")
        void passesCorrectParamsToRepository() {
            given(spotRepository.findAllInViewport(37.5, 37.6, 127.0, 127.1, PUBLISHED_CODE))
                .willReturn(List.of());

            spotQueryService.findSpotsInViewport(viewport);

            then(spotRepository).should().findAllInViewport(37.5, 37.6, 127.0, 127.1, PUBLISHED_CODE);
        }
    }

    // ── findSpots ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findSpots()")
    class FindSpots {

        @Test
        @DisplayName("위도만 전달하면 INVALID_INPUT_VALUE 예외를 던진다")
        void throwsWhenOnlyLatProvided() {
            assertThatThrownBy(() -> spotQueryService.findSpots(0, null, 37.5, null))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("경도만 전달하면 INVALID_INPUT_VALUE 예외를 던진다")
        void throwsWhenOnlyLngProvided() {
            assertThatThrownBy(() -> spotQueryService.findSpots(0, null, null, 127.0))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("위도/경도 모두 null이면 정상 처리된다")
        void successWhenBothCoordinatesNull() {
            given(spotMapper.findSpots(PUBLISHED_NAME, null, null, null, 0, 6))
                .willReturn(List.of());
            given(spotMapper.countSpots(PUBLISHED_NAME, null)).willReturn(0L);

            SpotListResponse response = spotQueryService.findSpots(0, null, null, null);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("위도/경도 모두 있으면 mapper에 전달하여 거리순으로 조회한다")
        void passesLatLngToMapper() {
            given(spotMapper.findSpots(PUBLISHED_NAME, null, 37.5, 127.0, 0, 6))
                .willReturn(List.of());
            given(spotMapper.countSpots(PUBLISHED_NAME, null)).willReturn(0L);

            spotQueryService.findSpots(0, null, 37.5, 127.0);

            then(spotMapper).should().findSpots(PUBLISHED_NAME, null, 37.5, 127.0, 0, 6);
        }

        @Test
        @DisplayName("테마 필터가 있으면 mapper에 theme 이름을 전달한다")
        void passesThemeToMapper() {
            given(spotMapper.findSpots(PUBLISHED_NAME, SpotTheme.SUNSET.name(), null, null, 0, 6))
                .willReturn(List.of());
            given(spotMapper.countSpots(PUBLISHED_NAME, SpotTheme.SUNSET.name())).willReturn(0L);

            spotQueryService.findSpots(0, SpotTheme.SUNSET, null, null);

            then(spotMapper).should().findSpots(PUBLISHED_NAME, SpotTheme.SUNSET.name(), null, null, 0, 6);
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
            given(spotMapper.findSpots(PUBLISHED_NAME, null, null, null, 0, 6)).willReturn(rows);
            given(spotMapper.countSpots(PUBLISHED_NAME, null)).willReturn(7L);
            given(spotImageRepository.findAllBySpotIdIn(any())).willReturn(List.of());

            SpotListResponse response = spotQueryService.findSpots(0, null, null, null);

            assertThat(response.hasNext()).isTrue();
        }

        @Test
        @DisplayName("전체 개수가 다음 페이지 기준 이하면 hasNext가 false다")
        void hasNextFalse_whenLastPage() {
            given(spotMapper.findSpots(PUBLISHED_NAME, null, null, null, 0, 6)).willReturn(List.of());
            given(spotMapper.countSpots(PUBLISHED_NAME, null)).willReturn(6L);

            SpotListResponse response = spotQueryService.findSpots(0, null, null, null);

            assertThat(response.hasNext()).isFalse();
        }

        @Test
        @DisplayName("page=1이면 offset=6으로 mapper를 호출한다")
        void passesCorrectOffsetForPage1() {
            given(spotMapper.findSpots(PUBLISHED_NAME, null, null, null, 6, 6)).willReturn(List.of());
            given(spotMapper.countSpots(PUBLISHED_NAME, null)).willReturn(0L);

            spotQueryService.findSpots(1, null, null, null);

            then(spotMapper).should().findSpots(PUBLISHED_NAME, null, null, null, 6, 6);
        }

        @Test
        @DisplayName("응답에 요청한 page 번호가 그대로 포함된다")
        void responseContainsRequestedPage() {
            given(spotMapper.findSpots(PUBLISHED_NAME, null, null, null, 12, 6)).willReturn(List.of());
            given(spotMapper.countSpots(PUBLISHED_NAME, null)).willReturn(0L);

            SpotListResponse response = spotQueryService.findSpots(2, null, null, null);

            assertThat(response.page()).isEqualTo(2);
        }

        @Test
        @DisplayName("SpotRow의 모든 필드가 SpotItem에 올바르게 매핑된다")
        void mapsSpotRowToSpotItemCorrectly() {
            SpotImage image = SpotImage.create(1L, "key.jpg");
            SpotRow row = new SpotRow(1L, "한강공원", "SS", 1.5);
            given(spotMapper.findSpots(PUBLISHED_NAME, null, null, null, 0, 6)).willReturn(List.of(row));
            given(spotMapper.countSpots(PUBLISHED_NAME, null)).willReturn(1L);
            given(spotImageRepository.findAllBySpotIdIn(List.of(1L))).willReturn(List.of(image));
            given(spotThumbnailService.getThumbnailUrl(image)).willReturn("https://cdn.example.com/thumb.jpg");

            SpotListResponse response = spotQueryService.findSpots(0, null, null, null);

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
            given(spotMapper.findSpots(PUBLISHED_NAME, null, null, null, 0, 6)).willReturn(List.of());
            given(spotMapper.countSpots(PUBLISHED_NAME, null)).willReturn(0L);

            spotQueryService.findSpots(0, null, null, null);

            then(spotImageRepository).should(never()).findAllBySpotIdIn(anyList());
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
