package com.ioes.photo.global.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;

/**
 * HTTP 클라이언트 편의성 유틸리티 컴포넌트 (RestClient 기반)
 * Spring 6의 RestClient를 기반으로 GET, POST, PUT, PATCH, DELETE 요청을 간편하게 수행할 수 있도록 래핑한 컴포넌트입니다
 *
 * @author 황제연
 */
@Component
@RequiredArgsConstructor
public class HttpClientUtils {

    private final RestClient restClient;

    public <T> T get(String url, Class<T> responseType) {
        return restClient.get()
            .uri(url)
            .retrieve()
            .body(responseType);
    }

    /**
     * 이미 인코딩된 URI로 GET 요청을 수행합니다.
     * 문자열 URL은 RestClient가 URI 템플릿으로 간주해 재인코딩하므로,
     * 한글 등 인코딩이 필요한 파라미터를 포함한 요청은 이 메서드를 사용해야 합니다.
     */
    public <T> T get(URI uri, Class<T> responseType) {
        return restClient.get()
            .uri(uri)
            .retrieve()
            .body(responseType);
    }

    public <T> T get(String url, ParameterizedTypeReference<T> responseType) {
        return restClient.get()
            .uri(url)
            .retrieve()
            .body(responseType);
    }

    public <T> T get(String url, Consumer<HttpHeaders> headersConsumer, Class<T> responseType) {
        return restClient.get()
            .uri(url)
            .headers(headersConsumer)
            .retrieve()
            .body(responseType);
    }

    public <T> T get(String url, Consumer<HttpHeaders> headersConsumer, ParameterizedTypeReference<T> responseType) {
        return restClient.get()
            .uri(url)
            .headers(headersConsumer)
            .retrieve()
            .body(responseType);
    }

    public <T> T get(URI uri, Consumer<HttpHeaders> headersConsumer, Class<T> responseType) {
        return restClient.get()
            .uri(uri)
            .headers(headersConsumer)
            .retrieve()
            .body(responseType);
    }

    public <T> T get(String urlTemplate, Map<String, ?> uriVariables, Class<T> responseType) {
        return restClient.get()
            .uri(urlTemplate, uriVariables)
            .retrieve()
            .body(responseType);
    }


    public <T> T post(String url, Object body, Class<T> responseType) {
        return restClient.post()
            .uri(url)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(responseType);
    }

    public <T> T post(String url, Object body, ParameterizedTypeReference<T> responseType) {
        return restClient.post()
            .uri(url)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(responseType);
    }

    public <T> T post(String url, Object body, Consumer<HttpHeaders> headersConsumer, Class<T> responseType) {
        return restClient.post()
            .uri(url)
            .headers(headersConsumer)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(responseType);
    }

    public void post(String url, Object body) {
        restClient.post()
            .uri(url)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toBodilessEntity();
    }

    public <T> T put(String url, Object body, Class<T> responseType) {
        return restClient.put()
            .uri(url)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(responseType);
    }

    public <T> T put(String url, Object body, Consumer<HttpHeaders> headersConsumer, Class<T> responseType) {
        return restClient.put()
            .uri(url)
            .headers(headersConsumer)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(responseType);
    }

    public void put(String url, Object body) {
        restClient.put()
            .uri(url)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toBodilessEntity();
    }

    public <T> T patch(String url, Object body, Class<T> responseType) {
        return restClient.patch()
            .uri(url)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(responseType);
    }

    public void patch(String url, Object body) {
        restClient.patch()
            .uri(url)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toBodilessEntity();
    }

    public <T> T delete(String url, Class<T> responseType) {
        return restClient.delete()
            .uri(url)
            .retrieve()
            .body(responseType);
    }

    public void delete(String url) {
        restClient.delete()
            .uri(url)
            .retrieve()
            .toBodilessEntity();
    }

    public void delete(String url, Consumer<HttpHeaders> headersConsumer) {
        restClient.delete()
            .uri(url)
            .headers(headersConsumer)
            .retrieve()
            .toBodilessEntity();
    }

    public static Consumer<HttpHeaders> bearer(String token) {
        return headers -> headers.setBearerAuth(token);
    }
}