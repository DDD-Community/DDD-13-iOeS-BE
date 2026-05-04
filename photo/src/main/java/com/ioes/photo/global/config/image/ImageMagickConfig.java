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
            } catch (IOException | InterruptedException ignored) {
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
    }
}