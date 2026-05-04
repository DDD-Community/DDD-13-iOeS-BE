package com.ioes.photo.domain.spot.service;

import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.global.storage.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * {@link SpotThumbnailService} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpotThumbnailService 단위 테스트")
class SpotThumbnailServiceTest {

    @Mock StorageService storageService;

    @InjectMocks SpotThumbnailService spotThumbnailService;

    @Nested
    @DisplayName("getThumbnailUrl()")
    class GetThumbnailUrl {

        @Test
        @DisplayName("thumbnailKey가 null이면 null을 반환한다")
        void returnsNull_whenThumbnailKeyIsNull() {
            SpotImage image = buildImage(null);

            String result = spotThumbnailService.getThumbnailUrl(image);

            assertThat(result).isNull();
            then(storageService).should(never()).getUrl(anyString());
        }

        @Test
        @DisplayName("thumbnailKey가 빈 문자열이면 null을 반환한다")
        void returnsNull_whenThumbnailKeyIsBlank() {
            SpotImage image = buildImage("   ");

            String result = spotThumbnailService.getThumbnailUrl(image);

            assertThat(result).isNull();
            then(storageService).should(never()).getUrl(anyString());
        }

        @Test
        @DisplayName("thumbnailKey가 있으면 storageService.getUrl()로 URL을 반환한다")
        void returnsUrl_whenThumbnailKeyPresent() {
            SpotImage image = buildImage("prod/public/spots/1/thumbnail/202504/abc.jpg");
            given(storageService.getUrl("prod/public/spots/1/thumbnail/202504/abc.jpg"))
                .willReturn("https://cdn.example.com/abc.jpg");

            String result = spotThumbnailService.getThumbnailUrl(image);

            assertThat(result).isEqualTo("https://cdn.example.com/abc.jpg");
        }
    }

    // ── helper ──────────────────────────────────────────────────────────────

    private static SpotImage buildImage(String thumbnailKey) {
        SpotImage image = SpotImage.create(1L, "original-key.jpg");
        if (thumbnailKey != null) {
            image.updateThumbnailKey(thumbnailKey);
        }
        return image;
    }


}
