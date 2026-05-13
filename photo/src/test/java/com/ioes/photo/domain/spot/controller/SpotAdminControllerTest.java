package com.ioes.photo.domain.spot.controller;

import com.ioes.photo.domain.spot.dto.SpotImageSyncRequest;
import com.ioes.photo.domain.spot.dto.SpotImageSyncResponse;
import com.ioes.photo.domain.spot.service.SpotImageAdminService;
import com.ioes.photo.global.common.response.ApiResponse;
import com.ioes.photo.global.error.exception.BusinessException;
import com.ioes.photo.global.error.code.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

/**
 * {@link SpotAdminController} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpotAdminController 단위 테스트")
class SpotAdminControllerTest {

    @Mock SpotImageAdminService spotImageAdminService;

    @InjectMocks SpotAdminController spotAdminController;

    private static final Long   SPOT_ID   = 1L;
    private static final String IMAGE_KEY = "prod/public/spots/1/original/202504/photo.jpg";

    // ── syncImage ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("syncImage()")
    class SyncImage {

        @Test
        @DisplayName("서비스에 spotId와 request를 그대로 전달한다")
        void delegatesToService_withCorrectArgs() {
            SpotImageSyncRequest request = new SpotImageSyncRequest(IMAGE_KEY, "photo.jpg", "image/jpeg", null, null);
            SpotImageSyncResponse serviceResponse = new SpotImageSyncResponse(
                "https://cdn.example.com/original.jpg",
                "https://cdn.example.com/thumbnail.jpg"
            );
            given(spotImageAdminService.syncImage(SPOT_ID, request)).willReturn(serviceResponse);

            spotAdminController.syncImage(SPOT_ID, request);

            then(spotImageAdminService).should().syncImage(SPOT_ID, request);
        }

        @Test
        @DisplayName("서비스 응답을 ApiResponse.success로 감싸서 반환한다")
        void wrapsServiceResponseInApiResponse() {
            SpotImageSyncRequest request = new SpotImageSyncRequest(IMAGE_KEY, "photo.jpg", "image/jpeg", null, null);
            SpotImageSyncResponse serviceResponse = new SpotImageSyncResponse(
                "https://cdn.example.com/original.jpg",
                "https://cdn.example.com/thumbnail.jpg"
            );
            given(spotImageAdminService.syncImage(SPOT_ID, request)).willReturn(serviceResponse);

            ApiResponse<SpotImageSyncResponse> response = spotAdminController.syncImage(SPOT_ID, request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().imageUrl()).isEqualTo("https://cdn.example.com/original.jpg");
            assertThat(response.getData().thumbnailUrl()).isEqualTo("https://cdn.example.com/thumbnail.jpg");
        }

        @Test
        @DisplayName("HEIC 이미지 동기화 요청도 서비스에 그대로 전달한다")
        void delegatesHeicRequest() {
            String heicKey = "prod/public/spots/1/original/202504/photo.heic";
            SpotImageSyncRequest request = new SpotImageSyncRequest(heicKey, "photo.heic", "image/heic", null, null);
            SpotImageSyncResponse serviceResponse = new SpotImageSyncResponse(
                "https://cdn.example.com/original.heic",
                "https://cdn.example.com/thumbnail.jpg"
            );
            given(spotImageAdminService.syncImage(SPOT_ID, request)).willReturn(serviceResponse);

            ApiResponse<SpotImageSyncResponse> response = spotAdminController.syncImage(SPOT_ID, request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().thumbnailUrl()).isEqualTo("https://cdn.example.com/thumbnail.jpg");
        }

        @Test
        @DisplayName("서비스에서 예외가 발생하면 그대로 전파된다")
        void propagatesExceptionFromService() {
            SpotImageSyncRequest request = new SpotImageSyncRequest(IMAGE_KEY, "photo.jpg", "image/jpeg", null, null);
            willThrow(new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "S3 오류"))
                .given(spotImageAdminService).syncImage(SPOT_ID, request);

            assertThatThrownBy(() -> spotAdminController.syncImage(SPOT_ID, request))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("spotId별로 독립적으로 서비스를 호출한다")
        void callsServiceWithCorrectSpotId() {
            Long anotherSpotId = 99L;
            SpotImageSyncRequest request = new SpotImageSyncRequest(IMAGE_KEY, "photo.jpg", "image/jpeg", null, null);
            SpotImageSyncResponse serviceResponse = new SpotImageSyncResponse(
                "https://cdn.example.com/url", "https://cdn.example.com/thumb"
            );
            given(spotImageAdminService.syncImage(anotherSpotId, request)).willReturn(serviceResponse);

            spotAdminController.syncImage(anotherSpotId, request);

            then(spotImageAdminService).should().syncImage(anotherSpotId, request);
        }
    }
}
