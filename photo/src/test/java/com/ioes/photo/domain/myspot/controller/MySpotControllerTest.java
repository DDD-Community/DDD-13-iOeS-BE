package com.ioes.photo.domain.myspot.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ioes.photo.domain.myspot.dto.MySpotListResponse;
import com.ioes.photo.domain.myspot.service.MySpotService;
import com.ioes.photo.global.common.response.ApiResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * {@link MySpotController} 단위 테스트.
 *
 * @author 김성민
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MySpotController 단위 테스트")
class MySpotControllerTest {

    @Mock MySpotService mySpotService;

    @InjectMocks MySpotController mySpotController;

    private static final Long USER_ID = 1L;

    private Authentication auth() {
        return new UsernamePasswordAuthenticationToken(String.valueOf(USER_ID), null, List.of());
    }

    @Nested
    @DisplayName("getMySpots()")
    class GetMySpots {

        @Test
        @DisplayName("서비스에 userId, page, lat, lng를 그대로 전달한다")
        void delegatesToService_withCorrectArgs() {
            given(mySpotService.findMySpots(USER_ID, 0, 37.5, 127.0))
                .willReturn(new MySpotListResponse(List.of(), 0, false));

            mySpotController.getMySpots(auth(), 0, 37.5, 127.0);

            then(mySpotService).should().findMySpots(USER_ID, 0, 37.5, 127.0);
        }

        @Test
        @DisplayName("서비스 응답을 ApiResponse.success로 감싸서 반환한다")
        void wrapsResponseInApiResponse() {
            given(mySpotService.findMySpots(USER_ID, 0, null, null))
                .willReturn(new MySpotListResponse(List.of(), 0, false));

            ApiResponse<MySpotListResponse> response =
                mySpotController.getMySpots(auth(), 0, null, null);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().spots()).isEmpty();
        }

        @Test
        @DisplayName("hasNext가 true이면 응답에도 true가 포함된다")
        void reflectsHasNext() {
            given(mySpotService.findMySpots(USER_ID, 0, null, null))
                .willReturn(new MySpotListResponse(List.of(), 0, true));

            ApiResponse<MySpotListResponse> response =
                mySpotController.getMySpots(auth(), 0, null, null);

            assertThat(response.getData().hasNext()).isTrue();
        }
    }
}
