package com.ioes.photo.domain.alarm.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ioes.photo.domain.alarm.dto.SpotAlarmResponse;
import com.ioes.photo.domain.alarm.dto.UpdateSpotAlarmRequest;
import com.ioes.photo.domain.alarm.service.SpotAlarmService;
import com.ioes.photo.global.common.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link MySpotAlarmController} 단위 테스트.
 *
 * @author 김성민
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MySpotAlarmController 단위 테스트")
class MySpotAlarmControllerTest {

    @Mock SpotAlarmService spotAlarmService;

    @InjectMocks MySpotAlarmController mySpotAlarmController;

    private static final Long USER_ID = 1L;
    private static final Long SPOT_ID = 10L;

    @Nested
    @DisplayName("getAlarm()")
    class GetAlarm {

        @Test
        @DisplayName("서비스 응답을 ApiResponse.success로 감싸 반환한다")
        void wrapsResponse() {
            given(spotAlarmService.getSubscription(USER_ID, SPOT_ID))
                .willReturn(new SpotAlarmResponse(SPOT_ID, true));

            ApiResponse<SpotAlarmResponse> response = mySpotAlarmController.getAlarm(USER_ID, SPOT_ID);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().enabled()).isTrue();
            then(spotAlarmService).should().getSubscription(USER_ID, SPOT_ID);
        }
    }

    @Nested
    @DisplayName("updateAlarm()")
    class UpdateAlarm {

        @Test
        @DisplayName("요청의 enabled 값을 서비스에 전달한다")
        void delegatesEnabledToService() {
            given(spotAlarmService.updateEnabled(USER_ID, SPOT_ID, false))
                .willReturn(new SpotAlarmResponse(SPOT_ID, false));

            ApiResponse<SpotAlarmResponse> response =
                mySpotAlarmController.updateAlarm(USER_ID, SPOT_ID, new UpdateSpotAlarmRequest(false));

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().enabled()).isFalse();
            then(spotAlarmService).should().updateEnabled(USER_ID, SPOT_ID, false);
        }
    }
}
