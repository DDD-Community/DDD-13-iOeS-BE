package com.ioes.photo.global.storage;

import com.ioes.photo.domain.storage.error.StorageErrorCode;
import com.ioes.photo.global.config.image.ImageProperties;
import com.ioes.photo.global.error.exception.BusinessException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 이미지 리사이즈 작업을 전용 스레드풀에서 실행하는 실행기.
 *
 * 리사이즈는 메모리를 많이 쓰는 작업이라 Tomcat 요청 스레드와 분리된 풀에서 실행하고,
 * 타임아웃을 둬서 비정상적으로 오래 걸리는 작업이 전체 풀을 점유하지 못하게 한다.
 *
 * @author 황제연
 */
@Slf4j
@Component
public class ImageResizeExecutor {

    private static final String MSG_TIMEOUT = "썸네일 생성이 시간 초과되었습니다.";
    private static final String MSG_GENERATION_FAILED = "썸네일 생성 중 오류가 발생했습니다.";
    private static final String MSG_INTERRUPTED = "썸네일 생성이 중단되었습니다.";

    private final ExecutorService executorService;

    public ImageResizeExecutor(ImageProperties imageProperties) {
        this.executorService = Executors.newFixedThreadPool(imageProperties.resize().poolSize());
    }

    public <T> T submitAndAwait(Callable<T> task, Duration timeout) {
        Future<T> future = executorService.submit(task);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new BusinessException(StorageErrorCode.THUMBNAIL_GENERATION_FAILED, MSG_TIMEOUT);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(StorageErrorCode.THUMBNAIL_GENERATION_FAILED, MSG_GENERATION_FAILED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(StorageErrorCode.THUMBNAIL_GENERATION_FAILED, MSG_INTERRUPTED);
        }
    }

    @PreDestroy
    void shutdown() {
        executorService.shutdown();
    }
}
