package com.ioes.photo.global.config.http;

import com.ioes.photo.global.config.http.properties.HttpClientProperties;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * RestClient 설정 클래스
 *
 *
 * @author 황제연
 */
@Slf4j
@Configuration
public class RestClientConfig {

    /**
     * 타임아웃 및 에러 핸들러가 설정된 {@link RestClient} 빈을 생성합니다.
     *
     * @param props HTTP 클라이언트 설정 프로퍼티
     * @return 설정된 RestClient 인스턴스
     */
    @Bean
    public RestClient restClient(HttpClientProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.connectTimeoutDuration());
        factory.setReadTimeout(props.readTimeoutDuration());

        return RestClient.builder()
            .requestFactory(factory)
            .defaultStatusHandler(
                    HttpStatusCode::is4xxClientError,
                (request, response) -> {
                    int code = response.getStatusCode().value();
                    log.warn("외부 API 4xx 오류: {} {}", code, request.getURI());
                    throw switch (code) {
                        case 401 -> new BusinessException(CommonErrorCode.UNAUTHORIZED,
                            "외부 API 인증 실패");
                        case 403 -> new BusinessException(CommonErrorCode.ACCESS_DENIED,
                            "외부 API 접근 거부");
                        case 404 -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND,
                            "외부 API 리소스를 찾을 수 없습니다");
                        default  -> new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE,
                            "외부 API 요청 오류: " + code);
                    };
                }
            )
            .defaultStatusHandler(
                    HttpStatusCode::is5xxServerError,
                (request, response) -> {
                    int code = response.getStatusCode().value();
                    log.error("외부 API 5xx 오류: {} {}", code, request.getURI());
                    throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR,
                        "외부 API 서버 오류: " + code);
                }
            )
            .build();
    }
}