package com.ioes.photo.global.config.image;

import com.ioes.photo.global.config.image.ImageMagickConfig.ImageMagickCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ImageMagickCommand} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("ImageMagickCommand 단위 테스트")
class ImageMagickCommandTest {

    @Nested
    @DisplayName("of()")
    class Of {

        @Test
        @DisplayName("명령어 문자열을 그대로 보관한다")
        void holdsCommandString() {
            ImageMagickCommand cmd = ImageMagickCommand.of("convert");
            assertThat(cmd.command()).isEqualTo("convert");
        }

        @Test
        @DisplayName("magick 명령어도 동일하게 보관한다")
        void holdsMagickCommand() {
            ImageMagickCommand cmd = ImageMagickCommand.of("magick");
            assertThat(cmd.command()).isEqualTo("magick");
        }
    }

    @Nested
    @DisplayName("absent()")
    class Absent {

        @Test
        @DisplayName("command가 null이다")
        void commandIsNull() {
            assertThat(ImageMagickCommand.absent().command()).isNull();
        }
    }

    @Nested
    @DisplayName("isPresent()")
    class IsPresent {

        @Test
        @DisplayName("of()로 생성하면 true를 반환한다")
        void trueWhenCreatedWithOf() {
            assertThat(ImageMagickCommand.of("convert").isPresent()).isTrue();
        }

        @Test
        @DisplayName("absent()로 생성하면 false를 반환한다")
        void falseWhenCreatedWithAbsent() {
            assertThat(ImageMagickCommand.absent().isPresent()).isFalse();
        }
    }
}
