package com.ioes.photo.global.logging;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/**
 * Async 태스크 실행 시 부모 스레드의 MDC 컨텍스트를 자식 스레드로 전파하는 데코레이터.
 *
 * MDC는 ThreadLocal 기반이므로 별도 처리 없이는 비동기 자식 스레드에서 requestId, userId 등의 컨텍스트 정보가 사라집니다
 * 이 클래스는 AsyncConfig의 ThreadPoolTaskExecutorㅇ 등록되어 모든 @Async 메서드에 자동으로 적용됩니다.
 *
 * 실행 흐름:
 * 1. 부모 스레드의 MDC 컨텍스트 복사
 * 2. 자식 스레드에서 복사된 컨텍스트 설정
 * 3. 태스크 실행
 * 4. finally에서 자식 스레드 MDC 초기화
 *
 *
 * @see com.ioes.photo.global.config.async.AsyncConfig
 * @see com.ioes.photo.global.logging.MdcFilter
 * @author 황제연
 */
public class MdcTaskDecorator implements TaskDecorator {

    /**
     * 부모 스레드의 MDC 컨텍스트를 전파하도록 Runnable을 감쌉니다.
     *
     * @param runnable 실행할 원본 태스크
     * @return MDC 컨텍스트 전파 로직이 추가된 Runnable
     */
    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}