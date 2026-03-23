package com.ioes.photo.global.config.async.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 비동기 스레드 풀 설정 프로퍼티 레코드
 *
 * @param corePoolSize     기본적으로 유지할 스레드 수
 * @param maxPoolSize      최대 생성 가능한 스레드 수 (큐가 가득 찼을 때까지만 증가)
 * @param queueCapacity    태스크 대기 큐 용량
 * @param threadNamePrefix 생성된 스레드 이름 접두사
 * @author 황제연
 */
@ConfigurationProperties(prefix = "app.async")
public record AsyncProperties(
    int corePoolSize,
    int maxPoolSize,
    int queueCapacity,
    String threadNamePrefix
) {}
