package com.ioes.photo.global.storage;

import com.ioes.photo.global.error.exception.BusinessException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ImageResizer} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("ImageResizer 단위 테스트")
class ImageResizerTest {

    private static final ImageResizer imageResizer = new ImageResizer();
    private static byte[] testPngBytes;

    @BeforeAll
    static void createTestImage() throws IOException {
        // 랜덤 픽셀로 채워 PNG 압축이 최대한 되지 않도록 하여 JPEG 리사이징 결과와 크기 비교 가능
        BufferedImage img = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
        java.util.Random rng = new java.util.Random(42);
        for (int y = 0; y < 500; y++) {
            for (int x = 0; x < 500; x++) {
                img.setRGB(x, y, rng.nextInt(0xFFFFFF));
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        testPngBytes = out.toByteArray();
    }

    // ── supports ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("supports()")
    class Supports {

        @Test
        @DisplayName("image/jpeg를 지원한다")
        void supportsJpeg() {
            assertThat(imageResizer.supports("image/jpeg")).isTrue();
        }

        @Test
        @DisplayName("image/jpg를 지원한다")
        void supportsJpg() {
            assertThat(imageResizer.supports("image/jpg")).isTrue();
        }

        @Test
        @DisplayName("image/png를 지원한다")
        void supportsPng() {
            assertThat(imageResizer.supports("image/png")).isTrue();
        }

        @Test
        @DisplayName("image/gif를 지원한다")
        void supportsGif() {
            assertThat(imageResizer.supports("image/gif")).isTrue();
        }

        @Test
        @DisplayName("image/bmp를 지원한다")
        void supportsBmp() {
            assertThat(imageResizer.supports("image/bmp")).isTrue();
        }

        @Test
        @DisplayName("image/webp를 지원한다")
        void supportsWebp() {
            assertThat(imageResizer.supports("image/webp")).isTrue();
        }

        @Test
        @DisplayName("대소문자 구분 없이 지원 여부를 판단한다")
        void caseInsensitive() {
            assertThat(imageResizer.supports("IMAGE/JPEG")).isTrue();
            assertThat(imageResizer.supports("Image/Png")).isTrue();
        }

        @Test
        @DisplayName("image/heic는 지원하지 않는다")
        void doesNotSupportHeic() {
            assertThat(imageResizer.supports("image/heic")).isFalse();
        }

        @Test
        @DisplayName("null이면 false를 반환한다")
        void returnsFalseForNull() {
            assertThat(imageResizer.supports(null)).isFalse();
        }

        @Test
        @DisplayName("빈 문자열이면 false를 반환한다")
        void returnsFalseForBlank() {
            assertThat(imageResizer.supports("   ")).isFalse();
        }

        @Test
        @DisplayName("알 수 없는 타입이면 false를 반환한다")
        void returnsFalseForUnknownType() {
            assertThat(imageResizer.supports("application/pdf")).isFalse();
        }
    }

    // ── outputContentType ─────────────────────────────────────────────────

    @Nested
    @DisplayName("outputContentType()")
    class OutputContentType {

        @Test
        @DisplayName("출력 타입은 항상 image/jpeg다")
        void returnsImageJpeg() {
            assertThat(imageResizer.outputContentType()).isEqualTo("image/jpeg");
        }
    }

    // ── resize ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("resize()")
    class Resize {

        @Test
        @DisplayName("유효한 PNG를 리사이징하면 비어있지 않은 JPEG 바이트를 반환한다")
        void resizesValidPng() {
            byte[] result = imageResizer.resize(testPngBytes, 50, 50);

            assertThat(result).isNotEmpty();
        }

        @Test
        @DisplayName("원본보다 작은 크기로 리사이징하면 원본보다 바이트가 적어진다")
        void reducesSize_whenResizingSmaller() {
            byte[] result = imageResizer.resize(testPngBytes, 10, 10);

            assertThat(result.length).isLessThan(testPngBytes.length);
        }

        @Test
        @DisplayName("유효하지 않은 바이트를 전달하면 BusinessException을 던진다")
        void throwsBusinessException_whenInvalidData() {
            byte[] invalid = new byte[]{0x00, 0x01, 0x02};

            assertThatThrownBy(() -> imageResizer.resize(invalid, 50, 50))
                .isInstanceOf(BusinessException.class);
        }
    }
}
