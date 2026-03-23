package com.ioes.photo.global.config.http.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * HTTP 클라이언트 타임아웃 설정 프로퍼티 레코드.
 *
 *
 * @param connectTimeout 서버 연결 대기 시간 (밀리초)
 * @param readTimeout    응답 데이터 읽기 대기 시간 (밀리초)
 * @author 황제연
 */
@ConfigurationProperties(prefix = "app.http-client")
public record HttpClientProperties(
    int connectTimeout,
    int readTimeout
) {
    /**
     * 커넥션 타임아웃을 Duration으로 변환합니다.
     *
     * @return 커넥션 타임아웃 Duration
     */
    public Duration connectTimeoutDuration() {
        return Duration.ofMillis(connectTimeout);
    }

    /**
     * 읽기 타임아웃을 Duration으로 변환합니다.
     *
     * @return 읽기 타임아웃 Duration
     */
    public Duration readTimeoutDuration() {
        return Duration.ofMillis(readTimeout);
    }
}
