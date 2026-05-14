package com.ioes.photo.domain.savedspot.controller;

import com.ioes.photo.domain.savedspot.dto.BookmarkResponse;
import com.ioes.photo.domain.savedspot.dto.SavedSpotListResponse;
import com.ioes.photo.domain.savedspot.service.SavedSpotService;
import com.ioes.photo.global.common.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * {@link SavedSpotController} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SavedSpotController 단위 테스트")
class SavedSpotControllerTest {

    @Mock SavedSpotService savedSpotService;

    @InjectMocks SavedSpotController savedSpotController;

    private static final Long USER_ID = 1L;
    private static final Long SPOT_ID = 10L;

    // ── addBookmark ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addBookmark()")
    class AddBookmark {

        @Test
        @DisplayName("userId와 spotId를 서비스에 그대로 전달한다")
        void delegatesToService_withCorrectArgs() {
            given(savedSpotService.addBookmark(USER_ID, SPOT_ID)).willReturn(new BookmarkResponse(5L));

            savedSpotController.addBookmark(USER_ID, SPOT_ID);

            then(savedSpotService).should().addBookmark(USER_ID, SPOT_ID);
        }

        @Test
        @DisplayName("서비스 응답을 ApiResponse.success로 감싸서 반환한다")
        void wrapsResponseInApiResponse() {
            given(savedSpotService.addBookmark(USER_ID, SPOT_ID)).willReturn(new BookmarkResponse(5L));

            ApiResponse<BookmarkResponse> response = savedSpotController.addBookmark(USER_ID, SPOT_ID);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().bookmarkCount()).isEqualTo(5L);
        }
    }

    // ── removeBookmark ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("removeBookmark()")
    class RemoveBookmark {

        @Test
        @DisplayName("userId와 spotId를 서비스에 그대로 전달한다")
        void delegatesToService_withCorrectArgs() {
            given(savedSpotService.removeBookmark(USER_ID, SPOT_ID)).willReturn(new BookmarkResponse(4L));

            savedSpotController.removeBookmark(USER_ID, SPOT_ID);

            then(savedSpotService).should().removeBookmark(USER_ID, SPOT_ID);
        }

        @Test
        @DisplayName("서비스 응답을 ApiResponse.success로 감싸서 반환한다")
        void wrapsResponseInApiResponse() {
            given(savedSpotService.removeBookmark(USER_ID, SPOT_ID)).willReturn(new BookmarkResponse(4L));

            ApiResponse<BookmarkResponse> response = savedSpotController.removeBookmark(USER_ID, SPOT_ID);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().bookmarkCount()).isEqualTo(4L);
        }
    }

    // ── getSavedSpots ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSavedSpots()")
    class GetSavedSpots {

        @Test
        @DisplayName("userId, page, lat, lng를 서비스에 그대로 전달한다")
        void delegatesToService_withCorrectArgs() {
            given(savedSpotService.findSavedSpots(USER_ID, 0, 37.5, 127.0))
                .willReturn(new SavedSpotListResponse(List.of(), 0, false));

            savedSpotController.getSavedSpots(USER_ID, 0, 37.5, 127.0);

            then(savedSpotService).should().findSavedSpots(USER_ID, 0, 37.5, 127.0);
        }

        @Test
        @DisplayName("서비스 응답을 ApiResponse.success로 감싸서 반환한다")
        void wrapsResponseInApiResponse() {
            given(savedSpotService.findSavedSpots(USER_ID, 0, null, null))
                .willReturn(new SavedSpotListResponse(List.of(), 0, false));

            ApiResponse<SavedSpotListResponse> response =
                savedSpotController.getSavedSpots(USER_ID, 0, null, null);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().spots()).isEmpty();
        }

        @Test
        @DisplayName("hasNext가 true이면 응답에도 true가 포함된다")
        void reflectsHasNext() {
            given(savedSpotService.findSavedSpots(USER_ID, 0, null, null))
                .willReturn(new SavedSpotListResponse(List.of(), 0, true));

            assertThat(savedSpotController.getSavedSpots(USER_ID, 0, null, null).getData().hasNext()).isTrue();
        }
    }
}
