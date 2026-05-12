package com.ioes.photo.domain.user.service;

import com.ioes.photo.domain.user.dto.ArchiveImageResponse;
import com.ioes.photo.domain.user.entity.User;
import com.ioes.photo.domain.user.error.UserErrorCode;
import com.ioes.photo.domain.user.repository.UserRepository;
import com.ioes.photo.global.auth.oauth.OAuthProvider;
import com.ioes.photo.global.error.exception.BusinessException;
import com.ioes.photo.global.storage.S3StorageService;
import com.ioes.photo.global.storage.StorageService;
import com.ioes.photo.global.storage.UploadResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * {@link UserArchiveService} 통합 테스트 — 실제 JPA + H2로 보관함 이미지 저장/조회 검증.
 *
 * @author 황제연
 */
@SpringBootTest
@DisplayName("UserArchiveService 통합 테스트")
class UserArchiveServiceIntegrationTest {

    private static final String PRESIGNED_URL = "https://s3.example.com/archive/presigned?token=abc";
    private static final String ARCHIVE_KEY = "dev/private/users/1/archive/202505/uuid.jpg";

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () ->
            "c2VjcmV0LWtleS1mb3ItdGVzdGluZy1wdXJwb3Nlcy1vbmx5LW11c3QtYmUtYXQtbGVhc3QtNjQtYnl0ZXMtbG9uZw==");
    }

    @MockitoBean RedisConnectionFactory redisConnectionFactory;
    @MockitoBean ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;
    @MockitoBean StorageService storageService;
    @MockitoBean S3StorageService s3StorageService;

    @Autowired UserArchiveService userArchiveService;
    @Autowired UserRepository userRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    @DisplayName("보관함 이미지를 업로드하면 DB에 archiveImageKey가 저장된다")
    void updateArchiveImage_savesKeyToDb() {
        User user = userRepository.save(buildUser());
        given(storageService.upload(any(), anyString()))
            .willReturn(new UploadResult(ARCHIVE_KEY, "photo.jpg", 9L, "image/jpeg"));
        given(storageService.getUrl(ARCHIVE_KEY)).willReturn(PRESIGNED_URL);

        MockMultipartFile imageFile = new MockMultipartFile(
            "archiveImage", "photo.jpg", "image/jpeg", "imagedata".getBytes()
        );
        ArchiveImageResponse response = userArchiveService.updateArchiveImage(user.getId(), imageFile);

        assertThat(response.archiveImageUrl()).isEqualTo(PRESIGNED_URL);
        User saved = userRepository.findById(user.getId()).orElseThrow();
        assertThat(saved.getArchiveImageKey()).isEqualTo(ARCHIVE_KEY);
    }

    @Test
    @DisplayName("이미지 업로드 후 조회하면 Presigned URL을 반환한다")
    void getArchiveImage_returnsPresignedUrl_afterUpdate() {
        User user = userRepository.save(buildUser());
        given(storageService.upload(any(), anyString()))
            .willReturn(new UploadResult(ARCHIVE_KEY, "photo.jpg", 9L, "image/jpeg"));
        given(storageService.getUrl(ARCHIVE_KEY)).willReturn(PRESIGNED_URL);

        MockMultipartFile imageFile = new MockMultipartFile(
            "archiveImage", "photo.jpg", "image/jpeg", "imagedata".getBytes()
        );
        userArchiveService.updateArchiveImage(user.getId(), imageFile);

        ArchiveImageResponse response = userArchiveService.getArchiveImage(user.getId());
        assertThat(response.archiveImageUrl()).isEqualTo(PRESIGNED_URL);
    }

    @Test
    @DisplayName("보관함 이미지가 없는 사용자를 조회하면 archiveImageUrl이 null이다")
    void getArchiveImage_returnsNull_whenNoImage() {
        User user = userRepository.save(buildUser());

        ArchiveImageResponse response = userArchiveService.getArchiveImage(user.getId());

        assertThat(response.archiveImageUrl()).isNull();
    }

    @Test
    @DisplayName("이미지를 교체하면 DB의 archiveImageKey가 새 키로 갱신된다")
    void updateArchiveImage_replacesKey_whenCalledTwice() {
        User user = userRepository.save(buildUser());
        String firstKey = "dev/private/users/1/archive/202505/first.jpg";
        String secondKey = "dev/private/users/1/archive/202505/second.jpg";
        MockMultipartFile imageFile = new MockMultipartFile(
            "archiveImage", "photo.jpg", "image/jpeg", "imagedata".getBytes()
        );

        given(storageService.upload(any(), anyString()))
            .willReturn(new UploadResult(firstKey, "photo.jpg", 9L, "image/jpeg"))
            .willReturn(new UploadResult(secondKey, "photo.jpg", 9L, "image/jpeg"));
        given(storageService.getUrl(anyString())).willReturn(PRESIGNED_URL);

        userArchiveService.updateArchiveImage(user.getId(), imageFile);
        userArchiveService.updateArchiveImage(user.getId(), imageFile);

        User saved = userRepository.findById(user.getId()).orElseThrow();
        assertThat(saved.getArchiveImageKey()).isEqualTo(secondKey);
    }

    @Test
    @DisplayName("존재하지 않는 사용자이면 USER_NOT_FOUND 예외를 던진다")
    void updateArchiveImage_throwsUserNotFound_whenUserMissing() {
        Long nonExistentId = 9999L;
        MockMultipartFile imageFile = new MockMultipartFile(
            "archiveImage", "photo.jpg", "image/jpeg", "imagedata".getBytes()
        );

        assertThatThrownBy(() -> userArchiveService.updateArchiveImage(nonExistentId, imageFile))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND));
    }

    private User buildUser() {
        return User.builder()
            .provider(OAuthProvider.KAKAO)
            .providerUserId("kakao-archive-test")
            .nickname("보관함테스트유저")
            .build();
    }
}