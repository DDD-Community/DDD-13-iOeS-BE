package com.ioes.photo.domain.user.controller;

import com.ioes.photo.domain.user.dto.ArchiveImageResponse;
import com.ioes.photo.domain.user.dto.UpdateArchiveNameRequest;
import com.ioes.photo.domain.user.error.UserErrorCode;
import com.ioes.photo.domain.user.service.UserArchiveService;
import com.ioes.photo.global.common.response.ApiResponse;
import com.ioes.photo.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * {@link UserArchiveController} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserArchiveController 단위 테스트")
class UserArchiveControllerTest {

    @Mock UserArchiveService userArchiveService;

    @InjectMocks UserArchiveController userArchiveController;

    private static final Long   USER_ID       = 1L;
    private static final String PRESIGNED_URL = "https://s3.example.com/archive/presigned?token=abc";
    private static final String ARCHIVE_NAME  = "나의 보관함";

    // ── updateArchiveImage ────────────────────────────────────────────────

    @Nested
    @DisplayName("updateArchiveImage()")
    class UpdateArchiveImage {

        @Test
        @DisplayName("이미지 업로드 성공 시 보관함 이름과 Presigned URL을 포함한 성공 응답을 반환한다")
        void shouldReturnSuccessWithPresignedUrl() {
            MockMultipartFile imageFile = new MockMultipartFile(
                "archiveImage", "photo.jpg", "image/jpeg", "imagedata".getBytes()
            );
            given(userArchiveService.updateArchiveImage(eq(USER_ID), any()))
                .willReturn(new ArchiveImageResponse(ARCHIVE_NAME, PRESIGNED_URL));

            ApiResponse<ArchiveImageResponse> response =
                userArchiveController.updateArchiveImage(USER_ID, imageFile);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().archiveName()).isEqualTo(ARCHIVE_NAME);
            assertThat(response.getData().archiveImageUrl()).isEqualTo(PRESIGNED_URL);
            then(userArchiveService).should().updateArchiveImage(eq(USER_ID), any());
        }

        @Test
        @DisplayName("서비스에서 예외가 발생하면 예외가 전파된다")
        void shouldPropagateException_whenServiceThrows() {
            MockMultipartFile imageFile = new MockMultipartFile(
                "archiveImage", "photo.jpg", "image/jpeg", "imagedata".getBytes()
            );
            given(userArchiveService.updateArchiveImage(eq(USER_ID), any()))
                .willThrow(new BusinessException(UserErrorCode.USER_NOT_FOUND));

            assertThatThrownBy(() -> userArchiveController.updateArchiveImage(USER_ID, imageFile))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND));
        }
    }

    // ── getArchiveImage ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getArchiveImage()")
    class GetArchiveImage {

        @Test
        @DisplayName("이미지가 있으면 보관함 이름과 Presigned URL을 포함한 성공 응답을 반환한다")
        void shouldReturnPresignedUrl_whenImageExists() {
            given(userArchiveService.getArchiveImage(USER_ID))
                .willReturn(new ArchiveImageResponse(ARCHIVE_NAME, PRESIGNED_URL));

            ApiResponse<ArchiveImageResponse> response =
                userArchiveController.getArchiveImage(USER_ID);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().archiveName()).isEqualTo(ARCHIVE_NAME);
            assertThat(response.getData().archiveImageUrl()).isEqualTo(PRESIGNED_URL);
        }

        @Test
        @DisplayName("이미지가 없으면 archiveImageUrl이 null인 성공 응답을 반환한다")
        void shouldReturnNullUrl_whenNoImage() {
            given(userArchiveService.getArchiveImage(USER_ID))
                .willReturn(new ArchiveImageResponse(ARCHIVE_NAME, null));

            ApiResponse<ArchiveImageResponse> response =
                userArchiveController.getArchiveImage(USER_ID);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().archiveName()).isEqualTo(ARCHIVE_NAME);
            assertThat(response.getData().archiveImageUrl()).isNull();
        }

        @Test
        @DisplayName("서비스에서 예외가 발생하면 예외가 전파된다")
        void shouldPropagateException_whenServiceThrows() {
            given(userArchiveService.getArchiveImage(USER_ID))
                .willThrow(new BusinessException(UserErrorCode.USER_NOT_FOUND));

            assertThatThrownBy(() -> userArchiveController.getArchiveImage(USER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND));
        }
    }

    // ── updateArchiveName ─────────────────────────────────────────────────

    @Nested
    @DisplayName("updateArchiveName()")
    class UpdateArchiveName {

        @Test
        @DisplayName("보관함 이름 수정 성공 시 변경된 이름을 포함한 성공 응답을 반환한다")
        void shouldReturnUpdatedName_whenSuccess() {
            String newName = "제주 여행 스팟";
            UpdateArchiveNameRequest request = new UpdateArchiveNameRequest(newName);
            given(userArchiveService.updateArchiveName(USER_ID, newName))
                .willReturn(new ArchiveImageResponse(newName, PRESIGNED_URL));

            ApiResponse<ArchiveImageResponse> response =
                userArchiveController.updateArchiveName(USER_ID, request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().archiveName()).isEqualTo(newName);
        }

        @Test
        @DisplayName("서비스에서 예외가 발생하면 예외가 전파된다")
        void shouldPropagateException_whenServiceThrows() {
            UpdateArchiveNameRequest request = new UpdateArchiveNameRequest("새 이름");
            given(userArchiveService.updateArchiveName(USER_ID, "새 이름"))
                .willThrow(new BusinessException(UserErrorCode.USER_NOT_FOUND));

            assertThatThrownBy(() -> userArchiveController.updateArchiveName(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND));
        }
    }
}
