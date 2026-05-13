package com.ioes.photo.domain.spot.controller;

import com.ioes.photo.domain.spot.dto.SpotDetailResponse;
import com.ioes.photo.domain.spot.dto.SpotListResponse;
import com.ioes.photo.domain.spot.dto.SpotListResponse.SpotItem;
import com.ioes.photo.domain.spot.dto.SpotViewportResponse;
import com.ioes.photo.domain.spot.dto.SpotViewportResponse.SpotSummary;
import com.ioes.photo.domain.spot.dto.ViewportRequest;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

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
                "https://cdn.example.com/original.jpg",
                null,
                LocalTime.of(18, 30),
                SkyStatus.CLEAR, PrecipitationType.NONE, 20,
                CongestionLevel.NORMAL, LocalTime.of(18, 55),
                null, null, null,
                "정보 없음", 0L, false, false
            );
            given(spotQueryService.findSpotDetail(1L, null)).willReturn(detail);

            ApiResponse<SpotDetailResponse> response = spotController.getSpotDetail(1L, null);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().spotId()).isEqualTo(1L);
            assertThat(response.getData().name()).isEqualTo("한강공원");
            assertThat(response.getData().sunsetTime()).isEqualTo(LocalTime.of(18, 55));
        }

        @Test
        @DisplayName("PathVariable spotId를 서비스에 그대로 전달한다")
        void passesSpotIdToService() {
            SpotDetailResponse detail = new SpotDetailResponse(
                42L, "테스트", null, SpotTheme.YUNSEUL, 37.5, 127.0, null,
                null, null, null, null, null, null, null, null, null, null, null,
                "정보 없음", 0L, false, false
            );
            given(spotQueryService.findSpotDetail(42L, null)).willReturn(detail);

            spotController.getSpotDetail(42L, null);

            then(spotQueryService).should().findSpotDetail(42L, null);
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

        @Test
        @DisplayName("인증된 사용자의 userId를 서비스에 전달한다")
        void passesAuthenticatedUserIdToService() {
            Authentication auth = new UsernamePasswordAuthenticationToken("42", null, List.of());
            SpotDetailResponse detail = new SpotDetailResponse(
                1L, "스팟", null, SpotTheme.YUNSEUL, 37.5, 127.0, null,
                null, null, null, null, null, null, null, null, null, null, null,
                "정보 없음", 0L, true, true
            );
            given(spotQueryService.findSpotDetail(1L, 42L)).willReturn(detail);

            spotController.getSpotDetail(1L, auth);

            then(spotQueryService).should().findSpotDetail(1L, 42L);
        }

        @Test
        @DisplayName("AnonymousAuthenticationToken이면 userId로 null을 전달한다")
        void passesNullUserId_whenAnonymous() {
            Authentication anonymous = new AnonymousAuthenticationToken(
                "anonymousKey", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
            SpotDetailResponse detail = new SpotDetailResponse(
                1L, "스팟", null, SpotTheme.YUNSEUL, 37.5, 127.0, null,
                null, null, null, null, null, null, null, null, null, null, null,
                "정보 없음", 0L, false, false
            );
            given(spotQueryService.findSpotDetail(1L, null)).willReturn(detail);

            spotController.getSpotDetail(1L, anonymous);

            then(spotQueryService).should().findSpotDetail(1L, null);
        }
    }

    // ── getSpotsInViewport ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getSpotsInViewport()")
    class GetSpotsInViewport {

        @Test
        @DisplayName("8개 꼭짓점 좌표로 ViewportRequest를 구성하여 서비스에 전달한다")
        void buildsViewportRequestFromEightParams() {
            SpotViewportResponse serviceResponse = new SpotViewportResponse(List.of());
            given(spotQueryService.findSpotsInViewport(any(ViewportRequest.class)))
                .willReturn(serviceResponse);

            spotController.getSpotsInViewport(
                37.6, 127.0,  // topLeft
                37.6, 127.1,  // topRight
                37.5, 127.0,  // bottomLeft
                37.5, 127.1   // bottomRight
            );

            ArgumentCaptor<ViewportRequest> captor = ArgumentCaptor.forClass(ViewportRequest.class);
            then(spotQueryService).should().findSpotsInViewport(captor.capture());

            ViewportRequest req = captor.getValue();
            assertThat(req.topLeftLat()).isEqualTo(37.6);
            assertThat(req.topLeftLng()).isEqualTo(127.0);
            assertThat(req.topRightLat()).isEqualTo(37.6);
            assertThat(req.topRightLng()).isEqualTo(127.1);
            assertThat(req.bottomLeftLat()).isEqualTo(37.5);
            assertThat(req.bottomLeftLng()).isEqualTo(127.0);
            assertThat(req.bottomRightLat()).isEqualTo(37.5);
            assertThat(req.bottomRightLng()).isEqualTo(127.1);
        }

        @Test
        @DisplayName("서비스 응답을 ApiResponse.success로 감싸서 반환한다")
        void wrapsServiceResponseInApiResponse() {
            List<SpotSummary> summaries = List.of(
                new SpotSummary(1L, "https://cdn.example.com/thumb.jpg", 37.55, 127.05)
            );
            given(spotQueryService.findSpotsInViewport(any(ViewportRequest.class)))
                .willReturn(new SpotViewportResponse(summaries));

            ApiResponse<SpotViewportResponse> response = spotController.getSpotsInViewport(
                37.6, 127.0, 37.6, 127.1, 37.5, 127.0, 37.5, 127.1
            );

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().spots()).hasSize(1);
            assertThat(response.getData().spots().get(0).spotId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("뷰포트 내 스팟이 없으면 빈 목록으로 성공 응답을 반환한다")
        void returnsEmptyList_whenNoSpots() {
            given(spotQueryService.findSpotsInViewport(any(ViewportRequest.class)))
                .willReturn(new SpotViewportResponse(List.of()));

            ApiResponse<SpotViewportResponse> response = spotController.getSpotsInViewport(
                37.6, 127.0, 37.6, 127.1, 37.5, 127.0, 37.5, 127.1
            );

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().spots()).isEmpty();
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
                new SpotItem(1L, "한강공원", "SS", "https://cdn.example.com/thumb.jpg", 1.2)
            );
            SpotListResponse serviceResponse = new SpotListResponse(items, 0, false);
            given(spotQueryService.findSpots(0, null, null, null)).willReturn(serviceResponse);

            ApiResponse<SpotListResponse> response = spotController.getSpots(0, null, null, null);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().spots()).hasSize(1);
            assertThat(response.getData().spots().get(0).name()).isEqualTo("한강공원");
        }

        @Test
        @DisplayName("테마 필터와 함께 호출하면 서비스에 theme을 그대로 전달한다")
        void passesThemeToService() {
            given(spotQueryService.findSpots(0, SpotTheme.SUNSET, null, null))
                .willReturn(new SpotListResponse(List.of(), 0, false));

            spotController.getSpots(0, SpotTheme.SUNSET, null, null);

            then(spotQueryService).should().findSpots(0, SpotTheme.SUNSET, null, null);
        }

        @Test
        @DisplayName("위도/경도와 함께 호출하면 서비스에 좌표를 그대로 전달한다")
        void passesCoordinatesToService() {
            given(spotQueryService.findSpots(0, null, 37.5, 127.0))
                .willReturn(new SpotListResponse(List.of(), 0, false));

            spotController.getSpots(0, null, 37.5, 127.0);

            then(spotQueryService).should().findSpots(0, null, 37.5, 127.0);
        }

        @Test
        @DisplayName("테마/좌표 없이 호출하면 서비스에 null을 전달한다")
        void passesNullsWhenNoFilterProvided() {
            given(spotQueryService.findSpots(eq(0), isNull(), isNull(), isNull()))
                .willReturn(new SpotListResponse(List.of(), 0, false));

            spotController.getSpots(0, null, null, null);

            then(spotQueryService).should().findSpots(0, null, null, null);
        }

        @Test
        @DisplayName("page 번호를 서비스에 그대로 전달한다")
        void passesPageNumberToService() {
            given(spotQueryService.findSpots(3, null, null, null))
                .willReturn(new SpotListResponse(List.of(), 3, false));

            ApiResponse<SpotListResponse> response = spotController.getSpots(3, null, null, null);

            assertThat(response.getData().page()).isEqualTo(3);
        }

        @Test
        @DisplayName("hasNext가 true이면 응답에도 true가 포함된다")
        void reflectsHasNextFromService() {
            given(spotQueryService.findSpots(0, null, null, null))
                .willReturn(new SpotListResponse(List.of(), 0, true));

            ApiResponse<SpotListResponse> response = spotController.getSpots(0, null, null, null);

            assertThat(response.getData().hasNext()).isTrue();
        }

        @Test
        @DisplayName("서비스 결과가 비어있으면 빈 목록으로 성공 응답을 반환한다")
        void returnsEmptyList_whenNoSpots() {
            given(spotQueryService.findSpots(0, null, null, null))
                .willReturn(new SpotListResponse(List.of(), 0, false));

            ApiResponse<SpotListResponse> response = spotController.getSpots(0, null, null, null);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().spots()).isEmpty();
        }
    }
}
