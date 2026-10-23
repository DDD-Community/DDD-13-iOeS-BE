package com.ioes.photo.global.storage;

import com.ioes.photo.domain.storage.error.StorageErrorCode;
import com.ioes.photo.global.common.util.NullUtils;
import com.ioes.photo.global.config.image.ImageProperties;
import com.ioes.photo.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.tasks.UnsupportedFormatException;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Iterator;
import java.util.Set;

/**
 * 이미지 썸네일 생성 컴포넌트.
 *
 * 원본 해상도 상한을 두지 않고도 OOM을 막기 위해 안전장치를 거친다.
 * - 추정 메모리가 vips-fallback-threshold-ratio를 넘는 단일 이미지는 반드시 {@link VipsImageResizer}로
 *   위임한다 — peak memory가 입력 크기가 아닌 출력 크기에 비례하는 엔진이라 혼자서도 힙을 위협하는
 *   이미지를 안전하게 처리할 수 있다. libvips가 설치돼 있지 않으면 기존 경로로 폴백하지 않고
 *   즉시 실패한다({@link VipsImageResizer#resize}).
 * - 그 외의 경우는 기존 경로를 그대로 쓴다.
 *   - {@link ImageResizeMemoryGate}: 예상 메모리가 예산을 넘지 않을 때만 디코딩을 시작한다.
 *   - {@link ImageResizeExecutor}: Tomcat 요청 스레드와 분리된 전용 풀에서 실행하고 타임아웃을 둔다.
 *
 * @author 황제연
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageResizer {

    private static final Set<String> SUPPORTED_TYPES = Set.of(
        "image/jpeg", "image/jpg", "image/png", "image/gif",
        "image/bmp", "image/webp"
    );

    private static final String OUTPUT_FORMAT = "jpeg";
    private static final String OUTPUT_CONTENT_TYPE = "image/jpeg";
    private static final double OUTPUT_QUALITY = 0.85;

    private static final String MSG_INTERRUPTED = "썸네일 생성이 중단되었습니다.";

    private final ImageResizeMemoryGate memoryGate;
    private final ImageResizeExecutor executor;
    private final VipsImageResizer vipsImageResizer;
    private final ImageProperties imageProperties;

    public byte[] resize(byte[] data, int width, int height) {
        long estimatedBytes = estimateDecodeBytes(data);

        if (memoryGate.exceedsVipsFallbackThreshold(estimatedBytes)) {
            return vipsImageResizer.resize(data, width, height);
        }

        acquireGate(estimatedBytes);
        try {
            Duration timeout = Duration.ofSeconds(imageProperties.resize().timeoutSeconds());
            return executor.submitAndAwait(() -> doResize(data, width, height), timeout);
        } finally {
            memoryGate.release(estimatedBytes);
        }
    }

    public boolean supports(String contentType) {
        if (NullUtils.isBlank(contentType)) {
            return false;
        }
        return SUPPORTED_TYPES.contains(contentType.toLowerCase());
    }

    public String outputContentType() {
        return OUTPUT_CONTENT_TYPE;
    }

    private void acquireGate(long estimatedBytes) {
        try {
            memoryGate.acquire(estimatedBytes);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(StorageErrorCode.THUMBNAIL_GENERATION_FAILED, MSG_INTERRUPTED);
        }
    }

    private long estimateDecodeBytes(byte[] data) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            if (iis == null) {
                throw new IOException("이미지 스트림을 열 수 없습니다.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new IOException("이미지 리더를 찾을 수 없습니다.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis);
                return ImageResizeMemoryGate.estimateDecodeBytes(reader.getWidth(0), reader.getHeight(0));
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new BusinessException(StorageErrorCode.UNSUPPORTED_IMAGE_FORMAT,
                "썸네일 변환을 지원하지 않는 이미지 포맷입니다: " + e.getMessage());
        }
    }

    private byte[] doResize(byte[] data, int width, int height) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(data);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Thumbnails.of(in)
                .size(width, height)
                .keepAspectRatio(true)
                .outputFormat(OUTPUT_FORMAT)
                .outputQuality(OUTPUT_QUALITY)
                .toOutputStream(out);
            return out.toByteArray();
        } catch (UnsupportedFormatException e) {
            throw new BusinessException(StorageErrorCode.UNSUPPORTED_IMAGE_FORMAT,
                "썸네일 변환을 지원하지 않는 이미지 포맷입니다: " + e.getMessage());
        } catch (IOException e) {
            throw new BusinessException(StorageErrorCode.THUMBNAIL_GENERATION_FAILED,
                "썸네일 생성 중 오류가 발생했습니다.");
        }
    }
}