package com.ioes.photo.global.storage;

import com.ioes.photo.domain.storage.error.StorageErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * HEIC/HEIF 이미지 리사이징 컴포넌트.
 *
 * Thumbnailator(ImageIO)가 지원하지 않는 HEIC/HEIF 포맷을
 * ImageMagick을 통해 JPEG로 변환하고 리사이징한다.
 *
 * 앱 시작 시 ImageMagick 설치 여부와 사용 가능한 명령어를 자동 감지한다.
 * - Linux/Docker: imagemagick + libheif 패키지 필요 (convert 명령어)
 * - Windows:      ImageMagick 7 설치 시 동작 (magick 명령어)
 * - 미설치 환경:  supports()가 false를 반환하며 HEIC 변환 비활성화
 *
 * @author 황제연
 */
@Slf4j
@Component
public class HeicImageResizer {

    private static final Set<String> SUPPORTED_TYPES = Set.of(
        "image/heic", "image/heif", "image/heic-sequence", "image/heif-sequence"
    );

    private static final int DETECT_TIMEOUT_SECONDS = 5;
    private static final int CONVERT_TIMEOUT_SECONDS = 30;
    private static final int JPEG_QUALITY = 85;

    private final Optional<String> convertCommand;

    public HeicImageResizer() {
        this.convertCommand = detectImageMagick();
    }

    private static Optional<String> detectImageMagick() {
        for (String cmd : new String[]{"convert", "magick"}) {
            try {
                Process p = new ProcessBuilder(cmd, "--version")
                    .redirectErrorStream(true)
                    .start();
                String output = new String(p.getInputStream().readAllBytes());
                boolean finished = p.waitFor(DETECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (finished && p.exitValue() == 0 && output.contains("ImageMagick")) {
                    log.info("ImageMagick 감지됨: '{}' 명령어로 HEIC 지원 활성화", cmd);
                    return Optional.of(cmd);
                }
            } catch (IOException | InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        log.warn("ImageMagick을 찾을 수 없어 HEIC 썸네일 생성이 비활성화됩니다.");
        return Optional.empty();
    }

    public boolean supports(String contentType) {
        return convertCommand.isPresent()
            && contentType != null
            && SUPPORTED_TYPES.contains(contentType.toLowerCase());
    }

    public byte[] resize(byte[] data, int width, int height) {
        // 유저가 스팟 등록할 수 있도록 활성화할 경우, 디스크에 임시파일 저장하는 방식으로 경로 명시 필수
        try (TempFile input = new TempFile("heic-in-", ".heic");
             TempFile output = new TempFile("heic-out-", ".jpg")) {

            Files.write(input.path, data);

            String geometry = width + "x" + height + ">";
            ProcessBuilder pb = new ProcessBuilder(
                convertCommand.orElseThrow(),
                "-resize", geometry,
                "-quality", String.valueOf(JPEG_QUALITY),
                input.path.toString(),
                output.path.toString()
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String processOutput = new String(process.getInputStream().readAllBytes());

            boolean finished = process.waitFor(CONVERT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException(StorageErrorCode.THUMBNAIL_GENERATION_FAILED, "HEIC 변환 시간 초과");
            }
            if (process.exitValue() != 0) {
                throw new BusinessException(StorageErrorCode.THUMBNAIL_GENERATION_FAILED, "HEIC 변환 실패: " + processOutput);
            }

            byte[] result = Files.readAllBytes(output.path);
            log.debug("HEIC 변환 완료: {}x{} → JPEG {} bytes", width, height, result.length);
            return result;

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(StorageErrorCode.THUMBNAIL_GENERATION_FAILED, "HEIC 변환 중 오류: " + e.getMessage());
        }
    }

    private static final class TempFile implements AutoCloseable {

        final Path path;

        TempFile(String prefix, String suffix) throws IOException {
            this.path = Files.createTempFile(prefix, suffix);
        }

        @Override
        public void close() {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {}
        }
    }
}