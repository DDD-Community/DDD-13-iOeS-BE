package com.ioes.photo.global.storage;

import com.ioes.photo.global.config.image.ImageProperties;
import com.ioes.photo.global.config.image.ImageProperties.ResizeProperties;
import com.ioes.photo.global.config.image.ImageProperties.ThumbnailProperties;
import com.ioes.photo.domain.storage.error.StorageErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

/**
 * {@link ImageResizer} 단위 테스트.
 *
 * {@link ImageResizeMemoryGate}, {@link ImageResizeExecutor}는 각각 전담 테스트가 있으므로
 * 여기서는 mock으로 대체하고, executor는 실제 콜백을 그대로 실행하도록 위임한다.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ImageResizer 단위 테스트")
class ImageResizerTest {

    @Mock ImageResizeMemoryGate memoryGate;
    @Mock ImageResizeExecutor executor;
    @Mock VipsImageResizer vipsImageResizer;

    private ImageResizer imageResizer;
    private static byte[] testPngBytes;

    @BeforeEach
    void setUp() throws Exception {
        ImageProperties imageProperties = new ImageProperties(
            new ThumbnailProperties(400, 400), 60, new ResizeProperties(4, 30, 0.5, 0.4));
        imageResizer = new ImageResizer(memoryGate, executor, vipsImageResizer, imageProperties);

        // supports()/outputContentType() 테스트나 입력 검증 단계에서 실패하는 테스트는 이 스텁을 쓰지 않으므로 lenient 처리
        Mockito.lenient().when(executor.submitAndAwait(any(), any())).thenAnswer(invocation -> {
            Callable<byte[]> task = invocation.getArgument(0);
            return task.call();
        });
        // 기본값: 임계값 미초과 → 기존 경로(게이트+실행기)를 그대로 탄다
        Mockito.lenient().when(memoryGate.exceedsVipsFallbackThreshold(anyLong())).thenReturn(false);
    }

    @BeforeAll
    static void createTestImage() throws IOException {
        // 랜덤 픽셀로 채워 PNG 압축이 최대한 되지 않도록 하여 JPEG 리사이징 결과와 크기 비교 가능
        BufferedImage img = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
        java.util.Random rng = new java.util.Random(42);
        for (int y = 0; y < 500; y++) {
            for (int x = 0; x < 500; x++) {
                img.setRGB(x, y, rng.nextInt(0xFFFFFF));
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        testPngBytes = out.toByteArray();
    }

    // ── supports ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("supports()")
    class Supports {

        @Test
        @DisplayName("image/jpeg를 지원한다")
        void supportsJpeg() {
            assertThat(imageResizer.supports("image/jpeg")).isTrue();
        }

        @Test
        @DisplayName("image/jpg를 지원한다")
        void supportsJpg() {
            assertThat(imageResizer.supports("image/jpg")).isTrue();
        }

        @Test
        @DisplayName("image/png를 지원한다")
        void supportsPng() {
            assertThat(imageResizer.supports("image/png")).isTrue();
        }

        @Test
        @DisplayName("image/gif를 지원한다")
        void supportsGif() {
            assertThat(imageResizer.supports("image/gif")).isTrue();
        }

        @Test
        @DisplayName("image/bmp를 지원한다")
        void supportsBmp() {
            assertThat(imageResizer.supports("image/bmp")).isTrue();
        }

        @Test
        @DisplayName("image/webp를 지원한다")
        void supportsWebp() {
            assertThat(imageResizer.supports("image/webp")).isTrue();
        }

        @Test
        @DisplayName("대소문자 구분 없이 지원 여부를 판단한다")
        void caseInsensitive() {
            assertThat(imageResizer.supports("IMAGE/JPEG")).isTrue();
            assertThat(imageResizer.supports("Image/Png")).isTrue();
        }

        @Test
        @DisplayName("image/heic는 지원하지 않는다")
        void doesNotSupportHeic() {
            assertThat(imageResizer.supports("image/heic")).isFalse();
        }

        @Test
        @DisplayName("null이면 false를 반환한다")
        void returnsFalseForNull() {
            assertThat(imageResizer.supports(null)).isFalse();
        }

        @Test
        @DisplayName("빈 문자열이면 false를 반환한다")
        void returnsFalseForBlank() {
            assertThat(imageResizer.supports("   ")).isFalse();
        }

        @Test
        @DisplayName("알 수 없는 타입이면 false를 반환한다")
        void returnsFalseForUnknownType() {
            assertThat(imageResizer.supports("application/pdf")).isFalse();
        }
    }

    // ── outputContentType ─────────────────────────────────────────────────

    @Nested
    @DisplayName("outputContentType()")
    class OutputContentType {

        @Test
        @DisplayName("출력 타입은 항상 image/jpeg다")
        void returnsImageJpeg() {
            assertThat(imageResizer.outputContentType()).isEqualTo("image/jpeg");
        }
    }

    // ── resize ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("resize()")
    class Resize {

        @Test
        @DisplayName("유효한 PNG를 리사이징하면 비어있지 않은 JPEG 바이트를 반환한다")
        void resizesValidPng() {
            byte[] result = imageResizer.resize(testPngBytes, 50, 50);

            assertThat(result).isNotEmpty();
        }

        @Test
        @DisplayName("원본보다 작은 크기로 리사이징하면 원본보다 바이트가 적어진다")
        void reducesSize_whenResizingSmaller() {
            byte[] result = imageResizer.resize(testPngBytes, 10, 10);

            assertThat(result.length).isLessThan(testPngBytes.length);
        }

        @Test
        @DisplayName("유효하지 않은 바이트를 전달하면 BusinessException을 던진다")
        void throwsBusinessException_whenInvalidData() {
            byte[] invalid = new byte[]{0x00, 0x01, 0x02};

            assertThatThrownBy(() -> imageResizer.resize(invalid, 50, 50))
                .isInstanceOf(BusinessException.class);
        }
    }

    // ── vips 위임 ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("resize() - vips 위임")
    class VipsDelegation {

        @Test
        @DisplayName("임계값을 넘으면 게이트/실행기를 거치지 않고 무조건 vips로 위임한다")
        void delegatesToVips_whenThresholdExceeded() throws InterruptedException {
            byte[] vipsResult = "vips-jpeg".getBytes();
            Mockito.when(memoryGate.exceedsVipsFallbackThreshold(anyLong())).thenReturn(true);
            Mockito.when(vipsImageResizer.resize(testPngBytes, 50, 50)).thenReturn(vipsResult);

            byte[] result = imageResizer.resize(testPngBytes, 50, 50);

            assertThat(result).isEqualTo(vipsResult);
            Mockito.verify(executor, Mockito.never()).submitAndAwait(any(), any());
            Mockito.verify(memoryGate, Mockito.never()).acquire(anyLong());
            Mockito.verify(memoryGate, Mockito.never()).release(anyLong());
        }

        @Test
        @DisplayName("vips가 예외를 던지면(미설치 포함) 그대로 전파하고 기존 경로로 폴백하지 않는다")
        void propagatesVipsException() {
            BusinessException vipsException = new BusinessException(
                StorageErrorCode.THUMBNAIL_GENERATION_FAILED, "vips 실패");
            Mockito.when(memoryGate.exceedsVipsFallbackThreshold(anyLong())).thenReturn(true);
            Mockito.when(vipsImageResizer.resize(testPngBytes, 50, 50)).thenThrow(vipsException);

            assertThatThrownBy(() -> imageResizer.resize(testPngBytes, 50, 50))
                .isSameAs(vipsException);
            Mockito.verify(executor, Mockito.never()).submitAndAwait(any(), any());
        }
    }
}
