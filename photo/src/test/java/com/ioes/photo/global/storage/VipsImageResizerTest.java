package com.ioes.photo.global.storage;

import com.ioes.photo.domain.storage.error.StorageErrorCode;
import com.ioes.photo.global.config.image.ImageProperties;
import com.ioes.photo.global.config.image.ImageProperties.ResizeProperties;
import com.ioes.photo.global.config.image.ImageProperties.ThumbnailProperties;
import com.ioes.photo.global.config.image.VipsConfig.VipsCommand;
import com.ioes.photo.global.error.exception.BusinessException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link VipsImageResizer} 단위 테스트.
 *
 * isAvailable()은 항상 검증하고, resize()는 실제 libvips(vipsthumbnail) 설치를 요구하므로
 * 설치돼 있지 않은 환경(로컬 Windows 등)에서는 스킵한다(HeicImageResizerTest와 같은 이유).
 * CI(ci.yml에서 libvips-tools 설치)와 운영 컨테이너(Dockerfile에서 vips-tools 설치)에서는
 * 실제로 실행되어 end-to-end로 검증된다.
 *
 * @author 황제연
 */
@DisplayName("VipsImageResizer 단위 테스트")
class VipsImageResizerTest {

    @Nested
    @DisplayName("isAvailable()")
    class IsAvailable {

        @Test
        @DisplayName("vips가 감지되면 true를 반환한다")
        void trueWhenCommandPresent() {
            VipsImageResizer resizer = resizerWith(VipsCommand.of("vipsthumbnail"));
            assertThat(resizer.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("vips가 없으면 false를 반환한다")
        void falseWhenCommandAbsent() {
            VipsImageResizer resizer = resizerWith(VipsCommand.absent());
            assertThat(resizer.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("vips가 없으면 resize() 호출 시 기존 경로로 폴백하지 않고 즉시 예외를 던진다")
        void resizeThrows_whenCommandAbsent() {
            VipsImageResizer resizer = resizerWith(VipsCommand.absent());

            assertThatThrownBy(() -> resizer.resize(createTestPng(10, 10), 5, 5))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(StorageErrorCode.THUMBNAIL_GENERATION_FAILED);
        }
    }

    @Nested
    @DisplayName("resize() - 실제 libvips 호출")
    class Resize {

        @Test
        @DisplayName("libvips가 설치돼 있으면 목표 크기 이하의 유효한 JPEG를 생성한다")
        void resizesRealImage_whenVipsInstalled() throws IOException {
            Assumptions.assumeTrue(isVipsInstalled(), "libvips(vipsthumbnail)가 설치돼 있지 않아 스킵합니다.");

            VipsImageResizer resizer = resizerWith(VipsCommand.of("vipsthumbnail"));
            byte[] source = createTestPng(2000, 1500);

            byte[] result = resizer.resize(source, 100, 100);

            assertThat(result).isNotEmpty();
            BufferedImage resized = ImageIO.read(new ByteArrayInputStream(result));
            assertThat(resized.getWidth()).isLessThanOrEqualTo(100);
            assertThat(resized.getHeight()).isLessThanOrEqualTo(100);
        }

        @Test
        @DisplayName("원본이 목표 크기보다 작으면 확대하지 않는다")
        void doesNotUpscale_whenSourceSmallerThanTarget() throws IOException {
            Assumptions.assumeTrue(isVipsInstalled(), "libvips(vipsthumbnail)가 설치돼 있지 않아 스킵합니다.");

            VipsImageResizer resizer = resizerWith(VipsCommand.of("vipsthumbnail"));
            byte[] source = createTestPng(50, 50);

            byte[] result = resizer.resize(source, 400, 400);

            BufferedImage resized = ImageIO.read(new ByteArrayInputStream(result));
            assertThat(resized.getWidth()).isLessThanOrEqualTo(50);
            assertThat(resized.getHeight()).isLessThanOrEqualTo(50);
        }

        @Test
        @DisplayName("투명 영역을 검정이 아닌 흰 배경으로 플래튼한다")
        void flattensTransparencyToWhite_notBlack() throws IOException {
            Assumptions.assumeTrue(isVipsInstalled(), "libvips(vipsthumbnail)가 설치돼 있지 않아 스킵합니다.");

            VipsImageResizer resizer = resizerWith(VipsCommand.of("vipsthumbnail"));
            byte[] source = createTransparentPng(200, 200);

            byte[] result = resizer.resize(source, 50, 50);

            BufferedImage resized = ImageIO.read(new ByteArrayInputStream(result));
            int corner = resized.getRGB(0, 0);
            int red = (corner >> 16) & 0xFF;
            int green = (corner >> 8) & 0xFF;
            int blue = corner & 0xFF;
            // JPEG 압축으로 완전한 255는 아닐 수 있으므로 "검정에 가깝지 않다"로 검증한다.
            assertThat(red).isGreaterThan(200);
            assertThat(green).isGreaterThan(200);
            assertThat(blue).isGreaterThan(200);
        }
    }

    // ── helper ──────────────────────────────────────────────────────────────

    private static VipsImageResizer resizerWith(VipsCommand command) {
        ImageProperties imageProperties = new ImageProperties(
            new ThumbnailProperties(400, 400), 60, new ResizeProperties(4, 30, 0.5, 0.4));
        return new VipsImageResizer(command, imageProperties);
    }

    private static boolean isVipsInstalled() {
        try {
            Process p = new ProcessBuilder("vipsthumbnail", "--version").redirectErrorStream(true).start();
            return p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static byte[] createTestPng(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private static byte[] createTransparentPng(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setComposite(java.awt.AlphaComposite.Clear);
        g.fillRect(0, 0, width, height);
        g.dispose();
        img.setRGB(width - 1, 0, new Color(10, 10, 10, 255).getRGB());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }
}
