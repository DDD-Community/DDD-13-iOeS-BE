package com.ioes.photo.global.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MdcTaskDecorator} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("MdcTaskDecorator 테스트")
class MdcTaskDecoratorTest {

    private final MdcTaskDecorator decorator = new MdcTaskDecorator();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("부모 스레드의 MDC 컨텍스트가 자식 Runnable로 전파됨")
    void propagatesMdcContext() throws InterruptedException {
        MDC.put("requestId", "test-req-123");
        MDC.put("userId", "user-42");

        AtomicReference<String> capturedRequestId = new AtomicReference<>();
        AtomicReference<String> capturedUserId = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Runnable original = () -> {
            capturedRequestId.set(MDC.get("requestId"));
            capturedUserId.set(MDC.get("userId"));
            latch.countDown();
        };

        Runnable decorated = decorator.decorate(original);

        Thread child = new Thread(decorated);
        child.start();
        latch.await();

        assertThat(capturedRequestId.get()).isEqualTo("test-req-123");
        assertThat(capturedUserId.get()).isEqualTo("user-42");
    }

    @Test
    @DisplayName("자식 Runnable 실행 후 MDC가 초기화됨")
    void clearsMdcAfterExecution() throws InterruptedException {
        MDC.put("requestId", "req-abc");

        AtomicReference<Map<String, String>> afterMdc = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Runnable original = () -> {};
        Runnable decorated = decorator.decorate(original);

        Thread child = new Thread(() -> {
            decorated.run();
            afterMdc.set(MDC.getCopyOfContextMap());
            latch.countDown();
        });
        child.start();
        latch.await();

        // 자식 스레드 실행 후 MDC는 비어있어야 함
        assertThat(afterMdc.get()).isNullOrEmpty();
    }

    @Test
    @DisplayName("부모 MDC가 비어있어도 자식 Runnable 정상 실행")
    void handlesNullMdcContext() throws InterruptedException {
        MDC.clear(); // 부모 MDC 없음

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Runnable original = latch::countDown;
        Runnable decorated = decorator.decorate(original);

        Thread child = new Thread(() -> {
            try {
                decorated.run();
            } catch (Throwable t) {
                error.set(t);
                latch.countDown();
            }
        });
        child.start();
        latch.await();

        assertThat(error.get()).isNull();
    }
}