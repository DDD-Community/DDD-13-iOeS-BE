package com.ioes.photo.global.storage;

import com.ioes.photo.global.config.image.ImageProperties;
import com.ioes.photo.global.config.image.ImageProperties.ResizeProperties;
import com.ioes.photo.global.config.image.ImageProperties.ThumbnailProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * {@link ImageResizeMemoryGate} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("ImageResizeMemoryGate 단위 테스트")
class ImageResizeMemoryGateTest {

    private static ImageResizeMemoryGate newGate(double memoryBudgetRatio) {
        return newGate(memoryBudgetRatio, 0.4);
    }

    private static ImageResizeMemoryGate newGate(double memoryBudgetRatio, double vipsFallbackThresholdRatio) {
        ImageProperties imageProperties = new ImageProperties(
            new ThumbnailProperties(400, 400), 60,
            new ResizeProperties(4, 30, memoryBudgetRatio, vipsFallbackThresholdRatio));
        return new ImageResizeMemoryGate(imageProperties);
    }

    @Nested
    @DisplayName("estimateDecodeBytes()")
    class EstimateDecodeBytes {

        @Test
        @DisplayName("가로 x 세로 x 4byte x 2 (디코딩 버퍼 + 작업 버퍼)로 계산한다")
        void calculatesExpectedFormula() {
            long bytes = ImageResizeMemoryGate.estimateDecodeBytes(6000, 4000);

            assertThat(bytes).isEqualTo(6000L * 4000 * 4 * 2);
        }
    }

    @Nested
    @DisplayName("acquire()/release()")
    class AcquireRelease {

        @Test
        @DisplayName("예산 내 요청은 즉시 통과한다")
        void passesImmediately_whenWithinBudget() throws InterruptedException {
            ImageResizeMemoryGate gate = newGate(1.0);

            gate.acquire(100);
            gate.release(100);
        }

        @Test
        @DisplayName("예산을 초과하는 두 번째 요청은 첫 번째가 release될 때까지 대기한다")
        void blocksSecondRequest_untilFirstReleases() throws InterruptedException {
            ImageResizeMemoryGate gate = newGate(0.5);
            long budget = (long) (Runtime.getRuntime().maxMemory() * 0.5);
            AtomicBoolean secondAcquired = new AtomicBoolean(false);

            gate.acquire(budget); // 예산 전체를 선점

            Thread second = new Thread(() -> {
                try {
                    gate.acquire(1); // 예산이 꽉 찼으니 release 전까지 대기해야 함
                    secondAcquired.set(true);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });
            second.start();

            await().during(Duration.ofMillis(300)).atMost(Duration.ofSeconds(1))
                .untilAsserted(() -> assertThat(secondAcquired.get()).isFalse());

            gate.release(budget);

            await().atMost(Duration.ofSeconds(2)).untilTrue(secondAcquired);
        }

        @Test
        @DisplayName("단일 작업이 예산을 초과해도 실행 중인 작업이 없으면 통과시켜 교착을 피한다")
        void allowsLoneOversizedJob_toAvoidDeadlock() throws InterruptedException {
            ImageResizeMemoryGate gate = newGate(0.01); // 예산을 극단적으로 작게 설정
            long farExceedsBudget = Runtime.getRuntime().maxMemory();

            gate.acquire(farExceedsBudget); // 예산보다 훨씬 크지만 혼자이므로 통과해야 한다
            gate.release(farExceedsBudget);
        }
    }

    @Nested
    @DisplayName("exceedsVipsFallbackThreshold()")
    class ExceedsVipsFallbackThreshold {

        @Test
        @DisplayName("추정 메모리가 임계값을 넘으면 true를 반환한다")
        void returnsTrue_whenEstimateExceedsThreshold() {
            ImageResizeMemoryGate gate = newGate(0.5, 0.4);
            long threshold = (long) (Runtime.getRuntime().maxMemory() * 0.4);

            assertThat(gate.exceedsVipsFallbackThreshold(threshold + 1)).isTrue();
        }

        @Test
        @DisplayName("추정 메모리가 임계값 이하이면 false를 반환한다")
        void returnsFalse_whenEstimateWithinThreshold() {
            ImageResizeMemoryGate gate = newGate(0.5, 0.4);
            long threshold = (long) (Runtime.getRuntime().maxMemory() * 0.4);

            assertThat(gate.exceedsVipsFallbackThreshold(threshold)).isFalse();
        }
    }
}
