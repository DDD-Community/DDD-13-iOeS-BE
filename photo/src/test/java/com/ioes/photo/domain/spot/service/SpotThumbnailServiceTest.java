package com.ioes.photo.domain.spot.service;

import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.enums.ImageSourceType;
import com.ioes.photo.global.storage.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

        @Test
        @DisplayName("외부 호스팅 이미지는 thumbnailKey가 없어도 원본 URL을 그대로 반환한다")
        void returnsOriginalUrl_whenExternal() {
            SpotImage image = buildExternalImage("http://tong.visitkorea.or.kr/cms2/website/20/1961920.jpg");

            String result = spotThumbnailService.getThumbnailUrl(image);

            assertThat(result).isEqualTo("http://tong.visitkorea.or.kr/cms2/website/20/1961920.jpg");
            then(storageService).should(never()).getUrl(anyString());
        }
    }

    @Nested
    @DisplayName("getImageUrl()")
    class GetImageUrl {

        @Test
        @DisplayName("외부 호스팅 이미지는 storageService를 거치지 않고 저장된 URL을 그대로 반환한다")
        void returnsExternalUrlDirectly() {
            SpotImage image = buildExternalImage("http://tong.visitkorea.or.kr/cms2/website/20/1961920.jpg");

            String result = spotThumbnailService.getImageUrl(image);

            assertThat(result).isEqualTo("http://tong.visitkorea.or.kr/cms2/website/20/1961920.jpg");
            then(storageService).should(never()).getUrl(anyString());
        }

        @Test
        @DisplayName("자사 스토리지 이미지는 storageService.getUrl()로 URL을 조합한다")
        void returnsStorageUrl_whenInternal() {
            SpotImage image = SpotImage.create(1L, "prod/public/spots/1/original/202504/abc.jpg");
            given(storageService.getUrl("prod/public/spots/1/original/202504/abc.jpg"))
                .willReturn("https://cdn.example.com/abc.jpg");

            String result = spotThumbnailService.getImageUrl(image);

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

    // EXTERNAL 행은 애플리케이션이 만들지 않고 데이터 적재용 SQL이 직접 세팅하므로,
    // 여기서는 그 상태를 흉내내기 위해서만 리플렉션으로 필드를 강제 설정한다.
    private static SpotImage buildExternalImage(String externalUrl) {
        SpotImage image = SpotImage.create(1L, externalUrl);
        ReflectionTestUtils.setField(image, "imageSourceType", ImageSourceType.EXTERNAL);
        return image;
    }
}
