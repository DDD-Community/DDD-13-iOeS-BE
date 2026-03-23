package com.ioes.photo.global.config.async;

import com.ioes.photo.global.config.async.properties.AsyncProperties;
import com.ioes.photo.global.logging.MdcTaskDecorator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 실행을 위한 스레드 풀 설정 클래스.
 *
 * AsyncConfigurer를 구현하여 기본 비동기 실행기를 커스터마이징합니다.
 * MdcTaskDecorator를 적용하여 비동기 스레드에서도 MDC 컨텍스트가 유지됩니다.
 *
 *
 * @see AsyncProperties
 * @see MdcTaskDecorator
 * @author 황제연
 */
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfig implements AsyncConfigurer {

    private final AsyncProperties asyncProperties;

    /**
     *  메소드 실행 시 사용할 기본 스레드 풀을 생성합니다.
     *
     * MdcTaskDecorator가 적용되어 부모 스레드의 MDC 컨텍스트가 자식 스레드(비동기 태스크)로 전파됩니다.
     *
     * @return 설정된 {@link ThreadPoolTaskExecutor}
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncProperties.corePoolSize());
        executor.setMaxPoolSize(asyncProperties.maxPoolSize());
        executor.setQueueCapacity(asyncProperties.queueCapacity());
        executor.setThreadNamePrefix(asyncProperties.threadNamePrefix());
        executor.setTaskDecorator(new MdcTaskDecorator());
        // 큐 초과 시 CallerRunsPolicy: 작업을 버리지 않고 호출 스레드에서 직접 실행
        // (AbortPolicy 기본값은 RejectedExecutionException을 발생시켜 작업이 조용히 사라짐)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
