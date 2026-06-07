package com.ioes.photo.global.storage;

import com.ioes.photo.global.config.image.ImageMagickConfig.ImageMagickCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link HeicImageResizer} 단위 테스트.
 *
 * ImageMagick 실행을 요구하는 resize()는 포함하지 않는다.
 *
 * @author 황제연
 */
@DisplayName("HeicImageResizer 단위 테스트")
class HeicImageResizerTest {

    // ── supports ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("supports()")
    class Supports {

        @Test
        @DisplayName("ImageMagick이 감지되고 image/heic이면 true를 반환한다")
        void trueForHeic_whenCommandPresent() {
            HeicImageResizer resizer = resizerWith("convert");
            assertThat(resizer.supports("image/heic")).isTrue();
        }

        @Test
        @DisplayName("ImageMagick이 감지되고 image/heif이면 true를 반환한다")
        void trueForHeif_whenCommandPresent() {
            HeicImageResizer resizer = resizerWith("convert");
            assertThat(resizer.supports("image/heif")).isTrue();
        }

        @Test
        @DisplayName("ImageMagick이 감지되고 image/heic-sequence이면 true를 반환한다")
        void trueForHeicSequence_whenCommandPresent() {
            HeicImageResizer resizer = resizerWith("convert");
            assertThat(resizer.supports("image/heic-sequence")).isTrue();
        }

        @Test
        @DisplayName("ImageMagick이 감지되고 image/heif-sequence이면 true를 반환한다")
        void trueForHeifSequence_whenCommandPresent() {
            HeicImageResizer resizer = resizerWith("convert");
            assertThat(resizer.supports("image/heif-sequence")).isTrue();
        }

        @Test
        @DisplayName("contentType 대소문자 구분 없이 지원 여부를 판단한다")
        void caseInsensitive() {
            HeicImageResizer resizer = resizerWith("convert");
            assertThat(resizer.supports("IMAGE/HEIC")).isTrue();
            assertThat(resizer.supports("Image/Heif")).isTrue();
        }

        @Test
        @DisplayName("ImageMagick이 없으면 HEIC이라도 false를 반환한다")
        void falseForHeic_whenCommandAbsent() {
            HeicImageResizer resizer = resizerWithout();
            assertThat(resizer.supports("image/heic")).isFalse();
        }

        @Test
        @DisplayName("contentType이 null이면 false를 반환한다")
        void falseForNullContentType() {
            HeicImageResizer resizer = resizerWith("convert");
            assertThat(resizer.supports(null)).isFalse();
        }

        @Test
        @DisplayName("HEIC 계열이 아닌 타입이면 ImageMagick이 있어도 false를 반환한다")
        void falseForNonHeicType() {
            HeicImageResizer resizer = resizerWith("convert");
            assertThat(resizer.supports("image/jpeg")).isFalse();
            assertThat(resizer.supports("image/png")).isFalse();
        }

        @Test
        @DisplayName("magick 명령어로 감지된 경우에도 지원 여부를 올바르게 반환한다")
        void trueForHeic_whenMagickCommand() {
            HeicImageResizer resizer = resizerWith("magick");
            assertThat(resizer.supports("image/heic")).isTrue();
        }
    }

    // ── helper ──────────────────────────────────────────────────────────────

    private static HeicImageResizer resizerWith(String cmd) {
        return new HeicImageResizer(ImageMagickCommand.of(cmd));
    }

    private static HeicImageResizer resizerWithout() {
        return new HeicImageResizer(ImageMagickCommand.absent());
    }
}
