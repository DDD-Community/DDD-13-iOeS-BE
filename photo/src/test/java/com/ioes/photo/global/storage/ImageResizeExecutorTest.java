package com.ioes.photo.global.storage;

import com.ioes.photo.domain.storage.error.StorageErrorCode;
import com.ioes.photo.global.config.image.ImageProperties;
import com.ioes.photo.global.config.image.ImageProperties.ResizeProperties;
import com.ioes.photo.global.config.image.ImageProperties.ThumbnailProperties;
import com.ioes.photo.global.error.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ImageResizeExecutor} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("ImageResizeExecutor 단위 테스트")
class ImageResizeExecutorTest {

    private ImageResizeExecutor executor;

    @BeforeEach
    void setUp() {
        ImageProperties imageProperties = new ImageProperties(
            new ThumbnailProperties(400, 400), 60, new ResizeProperties(2, 30, 0.5, 0.4));
        executor = new ImageResizeExecutor(imageProperties);
    }

    @AfterEach
    void shutdown() {
        executor.shutdown();
    }

    @Nested
    @DisplayName("submitAndAwait()")
    class SubmitAndAwait {

        @Test
        @DisplayName("정상 종료된 작업의 결과를 반환한다")
        void returnsResult_whenTaskCompletes() {
            String result = executor.submitAndAwait(() -> "ok", Duration.ofSeconds(1));

            assertThat(result).isEqualTo("ok");
        }

        @Test
        @DisplayName("타임아웃을 초과하면 BusinessException을 던진다")
        void throwsBusinessException_whenTimeout() {
            assertThatThrownBy(() -> executor.submitAndAwait(() -> {
                Thread.sleep(500);
                return "late";
            }, Duration.ofMillis(50)))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("작업이 BusinessException을 던지면 그 예외를 그대로 전달한다")
        void propagatesOriginalBusinessException_whenTaskFails() {
            BusinessException original = new BusinessException(
                StorageErrorCode.UNSUPPORTED_IMAGE_FORMAT, "테스트 오류");

            assertThatThrownBy(() -> executor.submitAndAwait(() -> { throw original; }, Duration.ofSeconds(1)))
                .isSameAs(original);
        }

        @Test
        @DisplayName("작업이 일반 예외를 던지면 THUMBNAIL_GENERATION_FAILED로 변환한다")
        void wrapsUnknownException_asThumbnailGenerationFailed() {
            assertThatThrownBy(() -> executor.submitAndAwait(() -> {
                throw new RuntimeException("예상치 못한 오류");
            }, Duration.ofSeconds(1)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(StorageErrorCode.THUMBNAIL_GENERATION_FAILED);
        }
    }
}
