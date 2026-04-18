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

import java.net.URI;

/**
 * RestClient 설정 클래스.
 *
 * JSON(application/json)과 XML(application/xml, text/xml) 응답을 모두 처리할 수 있습니다
 *
 * @author 황제연
 */
@Slf4j
@Configuration
public class RestClientConfig {

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
                    log.warn("외부 API 4xx 오류: {} {}", code, maskUri(request.getURI()));
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
                    log.error("외부 API 5xx 오류: {} {}", code, maskUri(request.getURI()));
                    throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR,
                        "외부 API 서버 오류: " + code);
                }
            )
            .build();
    }

    private static String maskUri(URI uri) {
        String raw = uri.toString();
        return raw.replaceAll("(?i)(serviceKey=)[^&]+", "$1****")
                  .replaceAll("(openapi\\.seoul\\.go\\.kr:\\d+/)[^/]+(/)", "$1****$2");
    }
}