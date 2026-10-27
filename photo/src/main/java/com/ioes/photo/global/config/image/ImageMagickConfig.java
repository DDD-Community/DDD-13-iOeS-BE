package com.ioes.photo.global.config.image;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 외부 프로그램인 ImageMagick 연결을 위한 설정
 *
 * @author 황제연
 */
@Slf4j
@Configuration
public class ImageMagickConfig {

    private static final int DETECT_TIMEOUT_SECONDS = 5;

    @Bean
    public ImageMagickCommand imageMagickCommand() {
        for (String cmd : new String[]{"convert", "magick"}) {
            try {
                Process p = new ProcessBuilder(cmd, "--version")
                    .redirectErrorStream(true)
                    .start();
                String output = new String(p.getInputStream().readAllBytes());
                boolean finished = p.waitFor(DETECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (finished && p.exitValue() == 0 && output.contains("ImageMagick")) {
                    log.info("ImageMagick 감지됨: '{}' 명령어로 HEIC 지원 활성화", cmd);
                    return ImageMagickCommand.of(cmd);
                }
            } catch (IOException ignored) {
                // ImageMagick 미설치 환경 — HEIC 변환 비활성화
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.warn("ImageMagick을 찾을 수 없어 HEIC 썸네일 생성이 비활성화됩니다.");
        return ImageMagickCommand.absent();
    }

    public record ImageMagickCommand(String command) {

        public static ImageMagickCommand of(String cmd) {
            return new ImageMagickCommand(cmd);
        }

        public static ImageMagickCommand absent() {
            return new ImageMagickCommand(null);
        }

        public boolean isPresent() {
            return command != null;
        }

        // isPresent()는 부팅 시점 캐시, checkAlive()는 매 호출마다 재검사(헬스체크/메트릭용).
        // waitFor를 먼저 걸어 timeout을 보장한 뒤에만 stdout을 읽는다 — 먼저 읽으면 프로세스가
        // 멈췄을 때 timeout 없이 readAllBytes()에서 무한정 블록될 수 있다.
        public boolean checkAlive() {
            if (command == null) {
                return false;
            }
            try {
                Process p = new ProcessBuilder(command, "--version")
                    .redirectErrorStream(true)
                    .start();
                boolean finished = p.waitFor(DETECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!finished) {
                    p.destroyForcibly();
                    return false;
                }
                String output = new String(p.getInputStream().readAllBytes());
                return p.exitValue() == 0 && output.contains("ImageMagick");
            } catch (IOException e) {
                return false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }
}