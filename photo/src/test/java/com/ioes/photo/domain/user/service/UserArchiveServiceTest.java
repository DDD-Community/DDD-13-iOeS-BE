package com.ioes.photo.domain.user.service;

import com.ioes.photo.domain.user.dto.ArchiveImageResponse;
import com.ioes.photo.domain.user.entity.User;
import com.ioes.photo.domain.user.error.UserErrorCode;
import com.ioes.photo.domain.user.repository.UserRepository;
import com.ioes.photo.global.auth.oauth.OAuthProvider;
import com.ioes.photo.global.error.exception.BusinessException;
import com.ioes.photo.global.storage.StorageCleanupEvent;
import com.ioes.photo.global.storage.StorageService;
import com.ioes.photo.global.storage.UploadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * {@link UserArchiveService} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserArchiveService 단위 테스트")
class UserArchiveServiceTest {

    @Mock UserRepository userRepository;
    @Mock StorageService storageService;
    @Mock Environment environment;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks UserArchiveService userArchiveService;

    private static final Long USER_ID = 1L;
    private static final String PRESIGNED_URL = "https://s3.example.com/archive/presigned?token=abc";
    private static final String ARCHIVE_KEY = "dev/private/users/1/archive/202505/uuid.jpg";

    private User buildUser(String archiveImageKey) {
        User user = User.builder()
            .provider(OAuthProvider.KAKAO)
            .providerUserId("kakao-123")
            .nickname("테스트유저")
            .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        if (archiveImageKey != null) {
            ReflectionTestUtils.setField(user, "archiveImageKey", archiveImageKey);
        }
        return user;
    }

    private MultipartFile imageFile() {
        return new MockMultipartFile("archiveImage", "photo.jpg", "image/jpeg", "imagedata".getBytes());
    }

    // ── updateArchiveImage ────────────────────────────────────────────────

    @Nested
    @DisplayName("updateArchiveImage()")
    class UpdateArchiveImage {

        @Test
        @DisplayName("이미지를 업로드하면 S3에 저장되고 Presigned URL을 반환한다")
        void shouldUploadAndReturnPresignedUrl() {
            given(environment.getActiveProfiles()).willReturn(new String[]{"dev"});
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(buildUser(null)));
            given(storageService.upload(any(), anyString()))
                .willReturn(new UploadResult(ARCHIVE_KEY, "photo.jpg", 9L, "image/jpeg"));
            given(storageService.getUrl(ARCHIVE_KEY)).willReturn(PRESIGNED_URL);

            ArchiveImageResponse response = userArchiveService.updateArchiveImage(USER_ID, imageFile());

            then(storageService).should().upload(any(MultipartFile.class), anyString());
            assertThat(response.archiveImageUrl()).isEqualTo(PRESIGNED_URL);
        }

        @Test
        @DisplayName("기존 이미지가 있으면 StorageCleanupEvent가 발행된다")
        void shouldPublishCleanupEvent_whenOldKeyExists() {
            given(environment.getActiveProfiles()).willReturn(new String[]{"dev"});
            String oldKey = "dev/private/users/1/archive/202504/old-uuid.jpg";
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(buildUser(oldKey)));
            given(storageService.upload(any(), anyString()))
                .willReturn(new UploadResult(ARCHIVE_KEY, "photo.jpg", 9L, "image/jpeg"));
            given(storageService.getUrl(anyString())).willReturn(PRESIGNED_URL);

            userArchiveService.updateArchiveImage(USER_ID, imageFile());

            ArgumentCaptor<StorageCleanupEvent> captor = ArgumentCaptor.forClass(StorageCleanupEvent.class);
            then(eventPublisher).should().publishEvent(captor.capture());
            assertThat(captor.getValue().key()).isEqualTo(oldKey);
        }

        @Test
        @DisplayName("기존 이미지가 없으면 StorageCleanupEvent가 발행되지 않는다")
        void shouldNotPublishCleanupEvent_whenNoOldKey() {
            given(environment.getActiveProfiles()).willReturn(new String[]{"dev"});
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(buildUser(null)));
            given(storageService.upload(any(), anyString()))
                .willReturn(new UploadResult(ARCHIVE_KEY, "photo.jpg", 9L, "image/jpeg"));
            given(storageService.getUrl(anyString())).willReturn(PRESIGNED_URL);

            userArchiveService.updateArchiveImage(USER_ID, imageFile());

            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("존재하지 않는 userId이면 USER_NOT_FOUND 예외를 던진다")
        void shouldThrow_whenUserNotFound() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userArchiveService.updateArchiveImage(USER_ID, imageFile()))
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
        @DisplayName("archiveImageKey가 있으면 Presigned URL을 반환한다")
        void shouldReturnPresignedUrl_whenKeyExists() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(buildUser(ARCHIVE_KEY)));
            given(storageService.getUrl(ARCHIVE_KEY)).willReturn(PRESIGNED_URL);

            ArchiveImageResponse response = userArchiveService.getArchiveImage(USER_ID);

            assertThat(response.archiveImageUrl()).isEqualTo(PRESIGNED_URL);
        }

        @Test
        @DisplayName("archiveImageKey가 없으면 archiveImageUrl이 null이다")
        void shouldReturnNull_whenNoKey() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(buildUser(null)));

            ArchiveImageResponse response = userArchiveService.getArchiveImage(USER_ID);

            assertThat(response.archiveImageUrl()).isNull();
            then(storageService).should(never()).getUrl(anyString());
        }

        @Test
        @DisplayName("존재하지 않는 userId이면 USER_NOT_FOUND 예외를 던진다")
        void shouldThrow_whenUserNotFound() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userArchiveService.getArchiveImage(USER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND));
        }
    }
}