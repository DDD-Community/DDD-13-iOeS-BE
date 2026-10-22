package com.ioes.photo.global.storage;

import com.ioes.photo.domain.storage.error.StorageErrorCode;
import com.ioes.photo.global.config.image.ImageProperties;
import com.ioes.photo.global.config.image.VipsConfig.VipsCommand;
import com.ioes.photo.global.error.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 고해상도 이미지의 OOM 위험을 피하기 위한 libvips 기반 썸네일 생성 컴포넌트.
 *
 * libvips는 JPEG 등에서 shrink-on-load를 지원해 디코딩 단계부터 목표 크기로 줄여 읽으므로
 * peak memory가 입력 해상도가 아니라 출력 크기에 비례한다. {@link ImageResizer}가 추정 메모리가
 * JVM 최대 힙의 일정 비율(vips-fallback-threshold-ratio)을 넘는 이미지는 모두 이 컴포넌트로 위임한다.
 * libvips가 설치되지 않은 환경에서는(isAvailable()이 false) resize() 호출 시 즉시 예외를 던진다 —
 * 이 구간의 이미지는 기존 Thumbnailator 경로로는 처리할 수 없다고 보기 때문에 폴백하지 않는다.
 *
 * vips 프로세스는 JVM 힙 밖(네이티브) 메모리를 쓰므로, 동시 실행 수를 리사이즈 스레드풀과
 * 같은 크기(resize.pool-size)로 제한해 컨테이너 메모리 상한을 넘기지 않게 한다.
 *
 * @author 황제연
 */
@Slf4j
@Component
public class VipsImageResizer {

    private static final int JPEG_QUALITY = 85;
    // vipsthumbnail에는 별도 --background 플래그가 없다. background는 VipsForeignSave 공통 옵션이라
    // -o 출력 포맷 문자열에 Q와 함께 넣어야 적용된다(배열 대괄호 문법은 파서가 거부해 스칼라로 넘긴다).
    // 기본값이 검정(0)이라, 흰 배경으로 명시해 투명 PNG를 JPEG로 변환(알파 플래튼)할 때 예측 가능한 결과를 보장한다.
    private static final String BACKGROUND_WHITE = "255";
    private static final String MSG_INTERRUPTED = "고해상도 썸네일 생성이 중단되었습니다.";
    private static final String MSG_UNAVAILABLE = "libvips(vipsthumbnail)가 설치되지 않아 고해상도 썸네일을 생성할 수 없습니다.";

    private final VipsCommand vipsCommand;
    private final ImageProperties imageProperties;
    private final Semaphore concurrencyLimit;

    public VipsImageResizer(VipsCommand vipsCommand, ImageProperties imageProperties) {
        this.vipsCommand = vipsCommand;
        this.imageProperties = imageProperties;
        this.concurrencyLimit = new Semaphore(imageProperties.resize().poolSize());
    }

    public boolean isAvailable() {
        return vipsCommand.isPresent();
    }

    public byte[] resize(byte[] data, int width, int height) {
        if (!isAvailable()) {
            throw new BusinessException(StorageErrorCode.THUMBNAIL_GENERATION_FAILED, MSG_UNAVAILABLE);
        }

        try {
            concurrencyLimit.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(StorageErrorCode.THUMBNAIL_GENERATION_FAILED, MSG_INTERRUPTED);
        }

        try (TempFile input = new TempFile("vips-in-", ".tmp");
             TempFile output = new TempFile("vips-out-", ".jpg");
             TempFile processLog = new TempFile("vips-log-", ".log")) {

            Files.write(input.path, data);

            ProcessBuilder pb = new ProcessBuilder(
                vipsCommand.command(),
                input.path.toString(),
                "-o", output.path + "[Q=" + JPEG_QUALITY + ",background=" + BACKGROUND_WHITE + "]",
                // ">" 접미사가 없으면 vipsthumbnail이 원본보다 작은 target size를 확대해버린다.
                "--size", width + "x" + height + ">"
            );
            // stdout을 파이프로 읽으면 OS 파이프 버퍼가 차 프로세스가 블록될 수 있고,
            // waitFor(timeout)이 그 블록을 끝까지 기다리게 된다. 파일로 리다이렉트해 피한다.
            pb.redirectErrorStream(true);
            pb.redirectOutput(processLog.path.toFile());

            Process process = pb.start();
            int timeoutSeconds = imageProperties.resize().timeoutSeconds();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException(StorageErrorCode.THUMBNAIL_GENERATION_FAILED, "고해상도 썸네일 생성 시간 초과");
            }
            if (process.exitValue() != 0) {
                String processOutput = Files.readString(processLog.path);
                throw new BusinessException(StorageErrorCode.THUMBNAIL_GENERATION_FAILED, "고해상도 썸네일 생성 실패: " + processOutput);
            }

            byte[] result = Files.readAllBytes(output.path);
            log.info("libvips로 고해상도 썸네일 생성 완료: {}x{} → JPEG {} bytes", width, height, result.length);
            return result;

        } catch (IOException e) {
            throw new BusinessException(StorageErrorCode.THUMBNAIL_GENERATION_FAILED, "고해상도 썸네일 생성 중 오류: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(StorageErrorCode.THUMBNAIL_GENERATION_FAILED, MSG_INTERRUPTED);
        } finally {
            concurrencyLimit.release();
        }
    }
}
