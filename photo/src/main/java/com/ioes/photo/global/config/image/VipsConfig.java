package com.ioes.photo.global.config.image;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 외부 프로그램인 libvips(vipsthumbnail) 연결을 위한 설정.
 *
 * @author 황제연
 */
@Slf4j
@Configuration
public class VipsConfig {

    private static final int DETECT_TIMEOUT_SECONDS = 5;
    private static final String COMMAND = "vipsthumbnail";

    @Bean
    public VipsCommand vipsCommand() {
        try {
            Process p = new ProcessBuilder(COMMAND, "--version")
                .redirectErrorStream(true)
                .start();
            String output = new String(p.getInputStream().readAllBytes());
            boolean finished = p.waitFor(DETECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (finished && p.exitValue() == 0) {
                log.info("libvips 감지됨({}): 고해상도 이미지 폴백 활성화", output.trim());
                return VipsCommand.of(COMMAND);
            }
        } catch (IOException ignored) {
            // vips 미설치 환경 — 기존 경로로만 처리
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.warn("libvips를 찾을 수 없어 고해상도 썸네일 폴백이 비활성화됩니다. 기존 경로로만 처리합니다.");
        return VipsCommand.absent();
    }

    public record VipsCommand(String command) {

        public static VipsCommand of(String cmd) {
            return new VipsCommand(cmd);
        }

        public static VipsCommand absent() {
            return new VipsCommand(null);
        }

        public boolean isPresent() {
            return command != null;
        }

        // isPresent()는 부팅 시점 캐시, checkAlive()는 매 호출마다 재검사(헬스체크/메트릭용).
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
                return p.exitValue() == 0;
            } catch (IOException e) {
                return false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }
}
