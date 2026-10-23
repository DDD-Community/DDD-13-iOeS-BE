package com.ioes.photo.global.config.image;

import com.ioes.photo.global.config.image.ImageMagickConfig.ImageMagickCommand;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ImageMagickCommand} 단위 테스트.
 *
 * checkAlive()의 "실제로 설치돼 있으면 true" 케이스는 로컬 Windows 등 ImageMagick 미설치
 * 환경에서는 스킵한다.
 *
 * @author 황제연
 */
@DisplayName("ImageMagickCommand 단위 테스트")
class ImageMagickCommandTest {

    private static final int DETECT_TIMEOUT_SECONDS = 5;

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

    @Nested
    @DisplayName("checkAlive()")
    class CheckAlive {

        @Test
        @DisplayName("absent()면 프로세스를 띄우지 않고 false를 반환한다")
        void falseWhenAbsent() {
            assertThat(ImageMagickCommand.absent().checkAlive()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 명령어면 false를 반환한다")
        void falseWhenCommandDoesNotExist() {
            ImageMagickCommand cmd = ImageMagickCommand.of("no-such-imagemagick-binary-xyz");
            assertThat(cmd.checkAlive()).isFalse();
        }

        @Test
        @DisplayName("실제 ImageMagick이 설치돼 있으면 true를 반환한다")
        void trueWhenImageMagickInstalled() {
            String cmd = detectInstalledCommand();
            Assumptions.assumeTrue(cmd != null, "ImageMagick이 설치돼 있지 않아 스킵합니다.");

            assertThat(ImageMagickCommand.of(cmd).checkAlive()).isTrue();
        }

        private String detectInstalledCommand() {
            for (String candidate : new String[]{"convert", "magick"}) {
                try {
                    Process p = new ProcessBuilder(candidate, "--version").redirectErrorStream(true).start();
                    String output = new String(p.getInputStream().readAllBytes());
                    boolean finished = p.waitFor(DETECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    if (finished && p.exitValue() == 0 && output.contains("ImageMagick")) {
                        return candidate;
                    }
                } catch (IOException ignored) {
                    // 다음 후보를 시도한다.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            return null;
        }
    }
}
