package com.ioes.photo.domain.spot.controller;

import com.ioes.photo.domain.spot.dto.SpotReportRequest;
import com.ioes.photo.domain.spot.dto.SpotReportResponse;
import com.ioes.photo.domain.spot.service.SpotReportService;
import com.ioes.photo.global.common.response.ApiResponse;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
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
 * {@link SpotReportController} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpotReportController 단위 테스트")
class SpotReportControllerTest {

    @Mock SpotReportService spotReportService;

    @InjectMocks SpotReportController spotReportController;

    private static final Long USER_ID   = 42L;
    private static final Long SPOT_ID   = 7L;
    private static final Long REPORT_ID = 100L;

    // ── report ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("report()")
    class ReportTest {

        @Test
        @DisplayName("userId와 spotId를 서비스에 그대로 전달한다")
        void passesUserIdAndSpotIdToService() {
            SpotReportRequest request = new SpotReportRequest("신고 내용입니다");
            given(spotReportService.report(USER_ID, SPOT_ID, request))
                .willReturn(new SpotReportResponse(REPORT_ID));

            spotReportController.report(USER_ID, SPOT_ID, request);

            then(spotReportService).should().report(USER_ID, SPOT_ID, request);
        }

        @Test
        @DisplayName("서비스 응답을 ApiResponse.success로 감싸서 반환한다")
        void wrapsServiceResponseInApiResponse() {
            SpotReportRequest request = new SpotReportRequest("이름이 틀렸어요");
            given(spotReportService.report(USER_ID, SPOT_ID, request))
                .willReturn(new SpotReportResponse(REPORT_ID));

            ApiResponse<SpotReportResponse> response =
                spotReportController.report(USER_ID, SPOT_ID, request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().reportId()).isEqualTo(REPORT_ID);
        }

        @Test
        @DisplayName("다른 spotId도 서비스에 그대로 전달한다")
        void passesAnotherSpotIdToService() {
            Long anotherSpotId = 99L;
            SpotReportRequest request = new SpotReportRequest("위치가 잘못됐어요");
            given(spotReportService.report(USER_ID, anotherSpotId, request))
                .willReturn(new SpotReportResponse(REPORT_ID));

            spotReportController.report(USER_ID, anotherSpotId, request);

            then(spotReportService).should().report(USER_ID, anotherSpotId, request);
        }

        @Test
        @DisplayName("서비스에서 BusinessException이 발생하면 그대로 전파된다")
        void propagatesBusinessExceptionFromService() {
            SpotReportRequest request = new SpotReportRequest("신고 내용입니다");
            willThrow(new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "스팟 없음"))
                .given(spotReportService).report(USER_ID, SPOT_ID, request);

            assertThatThrownBy(() -> spotReportController.report(USER_ID, SPOT_ID, request))
                .isInstanceOf(BusinessException.class);
        }
    }
}
