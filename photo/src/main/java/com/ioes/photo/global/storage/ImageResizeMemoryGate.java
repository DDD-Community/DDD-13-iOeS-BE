package com.ioes.photo.global.storage;

import com.ioes.photo.global.config.image.ImageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 이미지 리사이즈 작업의 동시 메모리 사용량을 제한하는 가중치 기반 어드미션 컨트롤러.
 *
 * 해상도 상한을 두지 않고도 OOM을 막기 위해, 작업 시작 전 예상 메모리를 계산해
 * 현재 예약된 메모리 총합과 더해 예산을 넘지 않을 때만 통과시킨다.
 * 단일 작업이 예산보다 크더라도 현재 실행 중인 작업이 없다면 그 작업만은 통과시켜
 * 교착 상태를 피한다.
 *
 * 추정 메모리가 vips-fallback-threshold-ratio를 넘는 단일 이미지는 {@link ImageResizer}가
 * 이 게이트를 거치지 않고 {@link VipsImageResizer}로 위임한다 — 혼자서도 힙을 위협하는
 * 이미지를 교착방지 예외 경로로 통과시키는 대신, peak memory가 입력 크기에 비례하지 않는
 * 엔진으로 보내기 위함이다.
 *
 * @author 황제연
 */
@Slf4j
@Component
public class ImageResizeMemoryGate {

    // Thumbnailator는 원본 디코딩 버퍼(ARGB 4byte/px)와 리사이즈 작업 버퍼를 동시에 유지해 실질 사용량이 2배다.
    private static final int BYTES_PER_PIXEL = 4;
    private static final int DECODE_MEMORY_FACTOR = 2;
    private static final long BYTES_PER_MB = 1024L * 1024;
    private static final int PERCENT_SCALE = 100;

    private final long budgetBytes;
    private final long vipsFallbackThresholdBytes;
    private final Lock lock = new ReentrantLock();
    private final Condition budgetAvailable = lock.newCondition();
    private long reservedBytes = 0;

    public ImageResizeMemoryGate(ImageProperties imageProperties) {
        double budgetRatio = imageProperties.resize().memoryBudgetRatio();
        double vipsFallbackRatio = imageProperties.resize().vipsFallbackThresholdRatio();
        this.budgetBytes = budgetBytesFor(budgetRatio);
        this.vipsFallbackThresholdBytes = budgetBytesFor(vipsFallbackRatio);
        log.info("이미지 리사이즈 메모리 예산: {}MB (JVM 최대 힙의 {}%), vips 폴백 임계값: {}MB ({}%)",
            budgetBytes / BYTES_PER_MB, (int) (budgetRatio * PERCENT_SCALE),
            vipsFallbackThresholdBytes / BYTES_PER_MB, (int) (vipsFallbackRatio * PERCENT_SCALE));

        if (vipsFallbackRatio >= budgetRatio) {
            log.warn("vips-fallback-threshold-ratio({})가 memory-budget-ratio({}) 이상입니다 — "
                    + "혼자인 고해상도 이미지가 vips로 위임되지 않고 게이트의 교착방지 경로를 다시 탈 수 있습니다.",
                vipsFallbackRatio, budgetRatio);
        }
    }

    public static long estimateDecodeBytes(int width, int height) {
        return (long) width * height * BYTES_PER_PIXEL * DECODE_MEMORY_FACTOR;
    }

    private static long budgetBytesFor(double ratio) {
        return (long) (Runtime.getRuntime().maxMemory() * ratio);
    }

    public boolean exceedsVipsFallbackThreshold(long estimatedBytes) {
        return estimatedBytes > vipsFallbackThresholdBytes;
    }

    public void acquire(long estimatedBytes) throws InterruptedException {
        lock.lock();
        try {
            while (reservedBytes > 0 && reservedBytes + estimatedBytes > budgetBytes) {
                budgetAvailable.await();
            }
            reservedBytes += estimatedBytes;
        } finally {
            lock.unlock();
        }
    }

    public void release(long estimatedBytes) {
        lock.lock();
        try {
            reservedBytes -= estimatedBytes;
            budgetAvailable.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
