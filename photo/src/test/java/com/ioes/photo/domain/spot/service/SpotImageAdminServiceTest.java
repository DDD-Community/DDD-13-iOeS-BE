package com.ioes.photo.domain.spot.service;

import com.ioes.photo.domain.spot.dto.SpotImageSyncRequest;
import com.ioes.photo.domain.spot.dto.SpotImageSyncResponse;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.global.config.image.ImageProperties;
import com.ioes.photo.global.config.image.ImageProperties.ThumbnailProperties;
import com.ioes.photo.global.storage.HeicImageResizer;
import com.ioes.photo.global.storage.ImageResizer;
import com.ioes.photo.global.storage.S3StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * {@link SpotImageAdminService} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpotImageAdminService 단위 테스트")
class SpotImageAdminServiceTest {

    @Mock SpotImageRepository spotImageRepository;
    @Mock S3StorageService    s3StorageService;
    @Mock ImageResizer        imageResizer;
    @Mock HeicImageResizer    heicImageResizer;
    @Mock ImageProperties     imageProperties;

    @InjectMocks SpotImageAdminService spotImageAdminService;

    private static final Long   SPOT_ID       = 1L;
    private static final String IMAGE_KEY     = "prod/public/spots/1/original/202504/photo.jpg";
    private static final byte[] ORIGINAL_DATA = new byte[]{1, 2, 3};
    private static final byte[] THUMBNAIL_DATA = new byte[]{4, 5, 6};

    @BeforeEach
    void setUpImageProperties() {
        given(imageProperties.thumbnail()).willReturn(new ThumbnailProperties(400, 400));
    }

    // ── syncImage: create vs update ─────────────────────────────────────────

    @Nested
    @DisplayName("syncImage() - SpotImage 생성/수정")
    class SyncImageCreateOrUpdate {

        @Test
        @DisplayName("SpotImage가 없으면 새로 생성하여 저장한다")
        void createsNewSpotImage_whenNotFound() {
            SpotImageSyncRequest request = new SpotImageSyncRequest(IMAGE_KEY, "photo.jpg", "image/jpeg", null, null);
            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.empty());
            given(spotImageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(s3StorageService.fetchBytes(IMAGE_KEY)).willReturn(ORIGINAL_DATA);
            given(heicImageResizer.supports("image/jpeg")).willReturn(false);
            given(imageResizer.resize(ORIGINAL_DATA, 400, 400)).willReturn(THUMBNAIL_DATA);
            given(imageResizer.outputContentType()).willReturn("image/jpeg");
            given(s3StorageService.getUrl(any())).willReturn("https://cdn.example.com/url");

            spotImageAdminService.syncImage(SPOT_ID, request);

            ArgumentCaptor<SpotImage> captor = ArgumentCaptor.forClass(SpotImage.class);
            then(spotImageRepository).should().save(captor.capture());
            assertThat(captor.getValue().getSpotId()).isEqualTo(SPOT_ID);
            assertThat(captor.getValue().getImageKey()).isEqualTo(IMAGE_KEY);
        }

        @Test
        @DisplayName("SpotImage가 이미 있으면 imageKey/filename/contentType을 업데이트하고 저장한다")
        void updatesExistingSpotImage_whenFound() {
            SpotImageSyncRequest request = new SpotImageSyncRequest(
                "prod/public/spots/1/original/202504/new.jpg", "new.jpg", "image/jpeg", null, null);
            SpotImage existing = SpotImage.create(SPOT_ID, "old-key.jpg", "old.jpg", "image/png");

            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.of(existing));
            given(spotImageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(s3StorageService.fetchBytes(any())).willReturn(ORIGINAL_DATA);
            given(heicImageResizer.supports("image/jpeg")).willReturn(false);
            given(imageResizer.resize(ORIGINAL_DATA, 400, 400)).willReturn(THUMBNAIL_DATA);
            given(imageResizer.outputContentType()).willReturn("image/jpeg");
            given(s3StorageService.getUrl(any())).willReturn("https://cdn.example.com/url");

            spotImageAdminService.syncImage(SPOT_ID, request);

            assertThat(existing.getImageKey()).isEqualTo("prod/public/spots/1/original/202504/new.jpg");
            assertThat(existing.getOriginalFilename()).isEqualTo("new.jpg");
            assertThat(existing.getContentType()).isEqualTo("image/jpeg");
        }
    }

    // ── syncImage: HEIC vs 일반 이미지 ──────────────────────────────────────

    @Nested
    @DisplayName("syncImage() - 리사이저 선택")
    class SyncImageResizerSelection {

        @Test
        @DisplayName("HEIC 이미지이면 heicImageResizer로 썸네일을 생성한다")
        void usesHeicResizer_whenHeicContentType() {
            SpotImageSyncRequest request = new SpotImageSyncRequest(
                "prod/public/spots/1/original/202504/photo.heic", "photo.heic", "image/heic", null, null);

            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.empty());
            given(spotImageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(s3StorageService.fetchBytes(any())).willReturn(ORIGINAL_DATA);
            given(heicImageResizer.supports("image/heic")).willReturn(true);
            given(heicImageResizer.resize(ORIGINAL_DATA, 400, 400)).willReturn(THUMBNAIL_DATA);
            given(imageResizer.outputContentType()).willReturn("image/jpeg");
            given(s3StorageService.getUrl(any())).willReturn("https://cdn.example.com/url");

            spotImageAdminService.syncImage(SPOT_ID, request);

            then(heicImageResizer).should().resize(ORIGINAL_DATA, 400, 400);
            then(imageResizer).should(org.mockito.Mockito.never()).resize(any(), any(int.class), any(int.class));
        }

        @Test
        @DisplayName("JPEG 이미지이면 imageResizer로 썸네일을 생성한다")
        void usesImageResizer_whenJpegContentType() {
            SpotImageSyncRequest request = new SpotImageSyncRequest(IMAGE_KEY, "photo.jpg", "image/jpeg", null, null);

            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.empty());
            given(spotImageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(s3StorageService.fetchBytes(IMAGE_KEY)).willReturn(ORIGINAL_DATA);
            given(heicImageResizer.supports("image/jpeg")).willReturn(false);
            given(imageResizer.resize(ORIGINAL_DATA, 400, 400)).willReturn(THUMBNAIL_DATA);
            given(imageResizer.outputContentType()).willReturn("image/jpeg");
            given(s3StorageService.getUrl(any())).willReturn("https://cdn.example.com/url");

            spotImageAdminService.syncImage(SPOT_ID, request);

            then(imageResizer).should().resize(ORIGINAL_DATA, 400, 400);
            then(heicImageResizer).should(org.mockito.Mockito.never()).resize(any(), any(int.class), any(int.class));
        }
    }

    // ── syncImage: thumbnailKey 경로 ─────────────────────────────────────────

    @Nested
    @DisplayName("syncImage() - 썸네일 키 생성")
    class ThumbnailKeyGeneration {

        @Test
        @DisplayName("7개 경로로 구성된 imageKey는 [4]번 세그먼트가 thumbnail로 치환되고 확장자가 .jpg가 된다")
        void replacesFifthSegmentAndExtension_whenKeyHasSevenParts() {
            // prod/public/spots/1/original/202504/photo.heic → prod/public/spots/1/thumbnail/202504/photo.jpg
            String heicKey = "prod/public/spots/1/original/202504/photo.heic";
            SpotImageSyncRequest request = new SpotImageSyncRequest(heicKey, "photo.heic", "image/heic", null, null);

            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.empty());
            given(spotImageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(s3StorageService.fetchBytes(heicKey)).willReturn(ORIGINAL_DATA);
            given(heicImageResizer.supports("image/heic")).willReturn(true);
            given(heicImageResizer.resize(ORIGINAL_DATA, 400, 400)).willReturn(THUMBNAIL_DATA);
            given(imageResizer.outputContentType()).willReturn("image/jpeg");
            given(s3StorageService.getUrl(any())).willReturn("https://cdn.example.com/url");

            spotImageAdminService.syncImage(SPOT_ID, request);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            then(s3StorageService).should().uploadBytes(eq(THUMBNAIL_DATA), keyCaptor.capture(), eq("image/jpeg"));
            assertThat(keyCaptor.getValue()).isEqualTo("prod/public/spots/1/thumbnail/202504/photo.jpg");
        }

        @Test
        @DisplayName("경로 구성이 7개 미만인 imageKey는 StoragePathUtils 기반 fallback 키를 사용한다")
        void usesFallbackKey_whenKeyHasFewerThanSevenParts() {
            String shortKey = "simple/key.jpg";
            SpotImageSyncRequest request = new SpotImageSyncRequest(shortKey, "key.jpg", "image/jpeg", null, null);

            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.empty());
            given(spotImageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(s3StorageService.fetchBytes(shortKey)).willReturn(ORIGINAL_DATA);
            given(heicImageResizer.supports("image/jpeg")).willReturn(false);
            given(imageResizer.resize(ORIGINAL_DATA, 400, 400)).willReturn(THUMBNAIL_DATA);
            given(imageResizer.outputContentType()).willReturn("image/jpeg");
            given(s3StorageService.getUrl(any())).willReturn("https://cdn.example.com/url");

            spotImageAdminService.syncImage(SPOT_ID, request);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            then(s3StorageService).should().uploadBytes(eq(THUMBNAIL_DATA), keyCaptor.capture(), eq("image/jpeg"));
            assertThat(keyCaptor.getValue()).contains("thumbnail");
        }
    }

    // ── syncImage: 응답 ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("syncImage() - 응답")
    class SyncImageResponse {

        @Test
        @DisplayName("syncImage 응답에 imageUrl과 thumbnailUrl이 포함된다")
        void returnsImageUrlAndThumbnailUrl() {
            SpotImageSyncRequest request = new SpotImageSyncRequest(IMAGE_KEY, "photo.jpg", "image/jpeg", null, null);
            String thumbnailKey = "prod/public/spots/1/thumbnail/202504/photo.jpg";

            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.empty());
            given(spotImageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(s3StorageService.fetchBytes(IMAGE_KEY)).willReturn(ORIGINAL_DATA);
            given(heicImageResizer.supports("image/jpeg")).willReturn(false);
            given(imageResizer.resize(ORIGINAL_DATA, 400, 400)).willReturn(THUMBNAIL_DATA);
            given(imageResizer.outputContentType()).willReturn("image/jpeg");
            given(s3StorageService.getUrl(IMAGE_KEY)).willReturn("https://cdn.example.com/original.jpg");
            given(s3StorageService.getUrl(thumbnailKey)).willReturn("https://cdn.example.com/thumbnail.jpg");

            SpotImageSyncResponse response = spotImageAdminService.syncImage(SPOT_ID, request);

            assertThat(response.imageUrl()).isEqualTo("https://cdn.example.com/original.jpg");
            assertThat(response.thumbnailUrl()).isEqualTo("https://cdn.example.com/thumbnail.jpg");
        }
    }

    // ── syncImage: 기록 시간 ──────────────────────────────────────────────────

    @Nested
    @DisplayName("syncImage() - 기록 시간")
    class SyncImageRecordedTime {

        @Test
        @DisplayName("recordedTime이 전달되면 SpotImage에 저장된다")
        void persistsRecordedTime_whenProvided() {
            LocalTime recordedAt = LocalTime.of(18, 30);
            SpotImageSyncRequest request = new SpotImageSyncRequest(
                IMAGE_KEY, "photo.jpg", "image/jpeg", null, recordedAt);

            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.empty());
            given(spotImageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(s3StorageService.fetchBytes(IMAGE_KEY)).willReturn(ORIGINAL_DATA);
            given(heicImageResizer.supports("image/jpeg")).willReturn(false);
            given(imageResizer.resize(ORIGINAL_DATA, 400, 400)).willReturn(THUMBNAIL_DATA);
            given(imageResizer.outputContentType()).willReturn("image/jpeg");
            given(s3StorageService.getUrl(any())).willReturn("https://cdn.example.com/url");

            spotImageAdminService.syncImage(SPOT_ID, request);

            ArgumentCaptor<SpotImage> captor = ArgumentCaptor.forClass(SpotImage.class);
            then(spotImageRepository).should().save(captor.capture());
            assertThat(captor.getValue().getRecordedTime()).isEqualTo(recordedAt);
        }

        @Test
        @DisplayName("기존 SpotImage가 있으면 recordedTime이 갱신된다")
        void updatesRecordedTimeOnExisting() {
            LocalTime recordedAt = LocalTime.of(5, 15);
            SpotImage existing = SpotImage.create(SPOT_ID, "old-key.jpg", "old.jpg", "image/png");
            SpotImageSyncRequest request = new SpotImageSyncRequest(
                IMAGE_KEY, "photo.jpg", "image/jpeg", null, recordedAt);

            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.of(existing));
            given(spotImageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(s3StorageService.fetchBytes(any())).willReturn(ORIGINAL_DATA);
            given(heicImageResizer.supports("image/jpeg")).willReturn(false);
            given(imageResizer.resize(ORIGINAL_DATA, 400, 400)).willReturn(THUMBNAIL_DATA);
            given(imageResizer.outputContentType()).willReturn("image/jpeg");
            given(s3StorageService.getUrl(any())).willReturn("https://cdn.example.com/url");

            spotImageAdminService.syncImage(SPOT_ID, request);

            assertThat(existing.getRecordedTime()).isEqualTo(recordedAt);
        }
    }
}
