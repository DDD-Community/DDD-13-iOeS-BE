package com.ioes.photo.domain.spot.controller;

import com.ioes.photo.domain.spot.dto.SpotDetailResponse;
import com.ioes.photo.domain.spot.dto.SpotListResponse;
import com.ioes.photo.domain.spot.dto.SpotListResponse.SpotItem;
import com.ioes.photo.domain.spot.dto.SpotPreviewResponse;
import com.ioes.photo.domain.spot.dto.SpotViewportResponse;
import com.ioes.photo.domain.spot.dto.SpotViewportResponse.SpotSummary;
import com.ioes.photo.domain.spot.dto.ViewportRequest;
import com.ioes.photo.domain.spot.enums.SortType;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.error.SpotErrorCode;
import com.ioes.photo.domain.spot.service.SpotQueryService;
import com.ioes.photo.external.crowd.enums.CongestionLevel;
import com.ioes.photo.external.weather.enums.PrecipitationType;
import com.ioes.photo.external.weather.enums.SkyStatus;
import com.ioes.photo.global.common.response.ApiResponse;
import com.ioes.photo.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * {@link SpotController} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpotController 단위 테스트")
class SpotControllerTest {

    @Mock SpotQueryService spotQueryService;

    @InjectMocks SpotController spotController;

    // ── getSpotDetail ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSpotDetail()")
    class GetSpotDetail {

        @Test
        @DisplayName("서비스 응답을 ApiResponse.success로 감싸서 반환한다")
        void wrapsServiceResponseInApiResponse() {
            SpotDetailResponse detail = new SpotDetailResponse(
                1L, "한강공원", "노을이 예쁜 곳", SpotTheme.SUNSET,
                37.55, 127.05, "서울시 마포구",
                "서울시 마포구 월드컵로 21", "서울시 마포구 망원동 1",
                "https://cdn.example.com/original.jpg",
                null, LocalTime.of(18, 30),
                SkyStatus.CLEAR, PrecipitationType.NONE, 20,
                CongestionLevel.NORMAL, LocalTime.of(18, 55),
                null, null, null,
                "정보 없음", 0L, false, false, "PUBLISHED", true, 0L, false, true, null
            );
            given(spotQueryService.findSpotDetail(1L, null)).willReturn(detail);

            ApiResponse<SpotDetailResponse> response = spotController.getSpotDetail(1L, null);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().spotId()).isEqualTo(1L);
            assertThat(response.getData().sunsetTime()).isEqualTo(LocalTime.of(18, 55));
        }

        @Test
        @DisplayName("userId를 서비스에 그대로 전달한다")
        void passesUserIdToService() {
            given(spotQueryService.findSpotDetail(1L, 42L))
                .willReturn(new SpotDetailResponse(
                    1L, "스팟", null, SpotTheme.YUNSEUL, 37.5, 127.0, null,
                    null, null,
                    null, null, null, null, null, null, null, null, null, null, null,
                    "정보 없음", 0L, true, true, "PUBLISHED", false, 0L, true, true, null
                ));

            spotController.getSpotDetail(1L, 42L);

            then(spotQueryService).should().findSpotDetail(1L, 42L);
        }

        @Test
        @DisplayName("비로그인(null userId)이면 서비스에 null을 전달한다")
        void passesNullUserId_whenNotAuthenticated() {
            given(spotQueryService.findSpotDetail(1L, null))
                .willReturn(new SpotDetailResponse(
                    1L, "스팟", null, SpotTheme.YUNSEUL, 37.5, 127.0, null,
                    null, null,
                    null, null, null, null, null, null, null, null, null, null, null,
                    "정보 없음", 0L, false, false, "PUBLISHED", true, 0L, false, true, null
                ));

            spotController.getSpotDetail(1L, null);

            then(spotQueryService).should().findSpotDetail(1L, null);
        }

        @Test
        @DisplayName("서비스에서 BusinessException이 발생하면 그대로 전파된다")
        void propagatesServiceException() {
            given(spotQueryService.findSpotDetail(99L, null))
                .willThrow(new BusinessException(SpotErrorCode.SPOT_NOT_FOUND));

            assertThatThrownBy(() -> spotController.getSpotDetail(99L, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SpotErrorCode.SPOT_NOT_FOUND);
        }
    }

    // ── getSpotsInViewport ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getSpotsInViewport()")
    class GetSpotsInViewport {

        @Test
        @DisplayName("8개 꼭짓점 좌표로 ViewportRequest를 구성하여 서비스에 전달한다")
        void buildsViewportRequestFromEightParams() {
            given(spotQueryService.findSpotsInViewport(any(ViewportRequest.class), isNull(), isNull()))
                .willReturn(new SpotViewportResponse(List.of()));

            spotController.getSpotsInViewport(
                37.6, 127.0, 37.6, 127.1, 37.5, 127.0, 37.5, 127.1, null, null
            );

            ArgumentCaptor<ViewportRequest> captor = ArgumentCaptor.forClass(ViewportRequest.class);
            then(spotQueryService).should().findSpotsInViewport(captor.capture(), isNull(), isNull());

            ViewportRequest req = captor.getValue();
            assertThat(req.topLeftLat()).isEqualTo(37.6);
            assertThat(req.topRightLng()).isEqualTo(127.1);
            assertThat(req.bottomRightLat()).isEqualTo(37.5);
        }

        @Test
        @DisplayName("theme과 userId를 서비스에 전달한다")
        void passesThemeAndUserIdToService() {
            given(spotQueryService.findSpotsInViewport(any(ViewportRequest.class), eq(SpotTheme.SUNSET), eq(42L)))
                .willReturn(new SpotViewportResponse(List.of()));

            spotController.getSpotsInViewport(
                37.6, 127.0, 37.6, 127.1, 37.5, 127.0, 37.5, 127.1, SpotTheme.SUNSET, 42L
            );

            then(spotQueryService).should().findSpotsInViewport(any(ViewportRequest.class), eq(SpotTheme.SUNSET), eq(42L));
        }

        @Test
        @DisplayName("스팟 목록을 ApiResponse.success로 감싸서 반환한다")
        void wrapsServiceResponseInApiResponse() {
            List<SpotSummary> summaries = List.of(
                new SpotSummary(1L, "https://cdn.example.com/thumb.jpg", 37.55, 127.05, false)
            );
            given(spotQueryService.findSpotsInViewport(any(ViewportRequest.class), isNull(), isNull()))
                .willReturn(new SpotViewportResponse(summaries));

            ApiResponse<SpotViewportResponse> response = spotController.getSpotsInViewport(
                37.6, 127.0, 37.6, 127.1, 37.5, 127.0, 37.5, 127.1, null, null
            );

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().spots()).hasSize(1);
        }
    }

    // ── getSpotPreview ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSpotPreview()")
    class GetSpotPreview {

        @Test
        @DisplayName("서비스 응답을 ApiResponse.success로 감싸서 반환한다")
        void wrapsServiceResponseInApiResponse() {
            SpotPreviewResponse preview = new SpotPreviewResponse(
                1L, "한강공원", false, SpotTheme.SUNSET, 5L, 1.2, null, "서울시 마포구", null, null, false, 3L, false, true, true
            );
            given(spotQueryService.findSpotPreview(1L, 37.5, 127.0, null)).willReturn(preview);

            ApiResponse<SpotPreviewResponse> response = spotController.getSpotPreview(1L, 37.5, 127.0, null);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().name()).isEqualTo("한강공원");
        }

        @Test
        @DisplayName("spotId, 위치, userId를 서비스에 그대로 전달한다")
        void passesAllParamsToService() {
            given(spotQueryService.findSpotPreview(1L, null, null, 42L))
                .willReturn(new SpotPreviewResponse(1L, "스팟", true, SpotTheme.SUNSET, 0L, null, null, null, null, null, false, 0L, false, true, false));

            spotController.getSpotPreview(1L, null, null, 42L);

            then(spotQueryService).should().findSpotPreview(1L, null, null, 42L);
        }
    }

    // ── getSpots ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSpots()")
    class GetSpots {

        @Test
        @DisplayName("서비스 응답을 ApiResponse.success로 감싸서 반환한다")
        void wrapsServiceResponseInApiResponse() {
            List<SpotItem> items = List.of(
                new SpotItem(1L, "한강공원", "SS", "https://cdn.example.com/thumb.jpg", 1.2, 7L, false, 3L, false)
            );
            given(spotQueryService.findSpots(0, null, null, null, SortType.RECOMMENDED, null))
                .willReturn(new SpotListResponse(items, 0, false));

            ApiResponse<SpotListResponse> response = spotController.getSpots(0, null, null, null, SortType.RECOMMENDED, null);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().spots().get(0).name()).isEqualTo("한강공원");
        }

        @Test
        @DisplayName("모든 파라미터를 서비스에 그대로 전달한다")
        void passesAllParamsToService() {
            given(spotQueryService.findSpots(2, SpotTheme.SUNSET, 37.5, 127.0, SortType.DISTANCE, null))
                .willReturn(new SpotListResponse(List.of(), 2, false));

            spotController.getSpots(2, SpotTheme.SUNSET, 37.5, 127.0, SortType.DISTANCE, null);

            then(spotQueryService).should().findSpots(2, SpotTheme.SUNSET, 37.5, 127.0, SortType.DISTANCE, null);
        }

        @Test
        @DisplayName("hasNext가 true이면 응답에도 true가 포함된다")
        void reflectsHasNextFromService() {
            given(spotQueryService.findSpots(0, null, null, null, SortType.RECOMMENDED, null))
                .willReturn(new SpotListResponse(List.of(), 0, true));

            assertThat(spotController.getSpots(0, null, null, null, SortType.RECOMMENDED, null).getData().hasNext()).isTrue();
        }
    }
}
