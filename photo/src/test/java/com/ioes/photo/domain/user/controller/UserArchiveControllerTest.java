package com.ioes.photo.domain.user.controller;

import com.ioes.photo.domain.user.dto.ArchiveImageResponse;
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
import org.springframework.security.core.Authentication;

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
    @Mock Authentication authentication;

    @InjectMocks UserArchiveController userArchiveController;

    private static final Long USER_ID = 1L;
    private static final String PRESIGNED_URL = "https://s3.example.com/archive/presigned?token=abc";

    // ── updateArchiveImage ────────────────────────────────────────────────

    @Nested
    @DisplayName("updateArchiveImage()")
    class UpdateArchiveImage {

        @Test
        @DisplayName("이미지 업로드 성공 시 Presigned URL을 포함한 성공 응답을 반환한다")
        void shouldReturnSuccessWithPresignedUrl() {
            given(authentication.getName()).willReturn(USER_ID.toString());
            MockMultipartFile imageFile = new MockMultipartFile(
                "archiveImage", "photo.jpg", "image/jpeg", "imagedata".getBytes()
            );
            given(userArchiveService.updateArchiveImage(eq(USER_ID), any()))
                .willReturn(ArchiveImageResponse.of(PRESIGNED_URL));

            ApiResponse<ArchiveImageResponse> response =
                userArchiveController.updateArchiveImage(authentication, imageFile);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().archiveImageUrl()).isEqualTo(PRESIGNED_URL);
            then(userArchiveService).should().updateArchiveImage(eq(USER_ID), any());
        }

        @Test
        @DisplayName("서비스에서 예외가 발생하면 예외가 전파된다")
        void shouldPropagateException_whenServiceThrows() {
            given(authentication.getName()).willReturn(USER_ID.toString());
            MockMultipartFile imageFile = new MockMultipartFile(
                "archiveImage", "photo.jpg", "image/jpeg", "imagedata".getBytes()
            );
            given(userArchiveService.updateArchiveImage(eq(USER_ID), any()))
                .willThrow(new BusinessException(UserErrorCode.USER_NOT_FOUND));

            assertThatThrownBy(() -> userArchiveController.updateArchiveImage(authentication, imageFile))
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
        @DisplayName("이미지가 있으면 Presigned URL을 포함한 성공 응답을 반환한다")
        void shouldReturnPresignedUrl_whenImageExists() {
            given(authentication.getName()).willReturn(USER_ID.toString());
            given(userArchiveService.getArchiveImage(USER_ID))
                .willReturn(ArchiveImageResponse.of(PRESIGNED_URL));

            ApiResponse<ArchiveImageResponse> response =
                userArchiveController.getArchiveImage(authentication);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().archiveImageUrl()).isEqualTo(PRESIGNED_URL);
        }

        @Test
        @DisplayName("이미지가 없으면 archiveImageUrl이 null인 성공 응답을 반환한다")
        void shouldReturnNullUrl_whenNoImage() {
            given(authentication.getName()).willReturn(USER_ID.toString());
            given(userArchiveService.getArchiveImage(USER_ID))
                .willReturn(ArchiveImageResponse.of(null));

            ApiResponse<ArchiveImageResponse> response =
                userArchiveController.getArchiveImage(authentication);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().archiveImageUrl()).isNull();
        }

        @Test
        @DisplayName("서비스에서 예외가 발생하면 예외가 전파된다")
        void shouldPropagateException_whenServiceThrows() {
            given(authentication.getName()).willReturn(USER_ID.toString());
            given(userArchiveService.getArchiveImage(USER_ID))
                .willThrow(new BusinessException(UserErrorCode.USER_NOT_FOUND));

            assertThatThrownBy(() -> userArchiveController.getArchiveImage(authentication))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND));
        }
    }
}