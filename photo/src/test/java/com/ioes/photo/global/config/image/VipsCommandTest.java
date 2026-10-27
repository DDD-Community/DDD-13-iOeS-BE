package com.ioes.photo.global.config.image;

import com.ioes.photo.global.config.image.VipsConfig.VipsCommand;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link VipsCommand} 단위 테스트.
 *
 * checkAlive()의 "실제로 설치돼 있으면 true" 케이스는 로컬 Windows 등 vips 미설치 환경에서는
 * 스킵한다(VipsImageResizerTest와 같은 이유).
 *
 * @author 황제연
 */
@DisplayName("VipsCommand 단위 테스트")
class VipsCommandTest {

    private static final int DETECT_TIMEOUT_SECONDS = 5;

    @Nested
    @DisplayName("of()")
    class Of {

        @Test
        @DisplayName("명령어 문자열을 그대로 보관한다")
        void holdsCommandString() {
            VipsCommand cmd = VipsCommand.of("vipsthumbnail");
            assertThat(cmd.command()).isEqualTo("vipsthumbnail");
        }
    }

    @Nested
    @DisplayName("absent()")
    class Absent {

        @Test
        @DisplayName("command가 null이다")
        void commandIsNull() {
            assertThat(VipsCommand.absent().command()).isNull();
        }
    }

    @Nested
    @DisplayName("isPresent()")
    class IsPresent {

        @Test
        @DisplayName("of()로 생성하면 true를 반환한다")
        void trueWhenCreatedWithOf() {
            assertThat(VipsCommand.of("vipsthumbnail").isPresent()).isTrue();
        }

        @Test
        @DisplayName("absent()로 생성하면 false를 반환한다")
        void falseWhenCreatedWithAbsent() {
            assertThat(VipsCommand.absent().isPresent()).isFalse();
        }
    }

    @Nested
    @DisplayName("checkAlive()")
    class CheckAlive {

        @Test
        @DisplayName("absent()면 프로세스를 띄우지 않고 false를 반환한다")
        void falseWhenAbsent() {
            assertThat(VipsCommand.absent().checkAlive()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 명령어면 false를 반환한다")
        void falseWhenCommandDoesNotExist() {
            VipsCommand cmd = VipsCommand.of("no-such-vips-binary-xyz");
            assertThat(cmd.checkAlive()).isFalse();
        }

        @Test
        @DisplayName("실제 libvips가 설치돼 있으면 true를 반환한다")
        void trueWhenVipsInstalled() {
            Assumptions.assumeTrue(isVipsInstalled(), "libvips(vipsthumbnail)가 설치돼 있지 않아 스킵합니다.");

            VipsCommand cmd = VipsCommand.of("vipsthumbnail");
            assertThat(cmd.checkAlive()).isTrue();
        }

        private boolean isVipsInstalled() {
            try {
                Process p = new ProcessBuilder("vipsthumbnail", "--version").redirectErrorStream(true).start();
                return p.waitFor(DETECT_TIMEOUT_SECONDS, TimeUnit.SECONDS) && p.exitValue() == 0;
            } catch (IOException e) {
                return false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }
}
