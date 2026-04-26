package com.ioes.photo.external.crowd;

import com.ioes.photo.external.config.properties.ExternalApiProperties;
import com.ioes.photo.external.crowd.dto.CrowdStatusResponse;
import com.ioes.photo.external.error.ExternalApiErrorCode;
import com.ioes.photo.global.common.util.HttpClientUtils;
import com.ioes.photo.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.ResourceAccessException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 서울시 실시간 인구 데이터 API 클라이언트.
 *
 * <p>서울 열린데이터광장의 실시간 도시데이터 API를 호출하여
 * 장소별 혼잡도, 실시간/예측 인구, 도로 교통 정보를 조회합니다.</p>
 *
 * @author 김성민
 * @see CrowdStatusResponse
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeoulCrowdApiClient {

    private static final String API_NAME = "서울시 혼잡도";

    private final HttpClientUtils httpClientUtils;
    private final ExternalApiProperties properties;

    /**
     * 서울시 실시간 인구 데이터 조회.
     *
     * <p>외부 API 일시 장애에 대해 최대 3회 재시도(1s → 2s → 4s 백오프)한다.</p>
     *
     * @param areaName 장소명 (예: "광화문·덕수궁")
     * @return 혼잡도 응답
     */
    @Retryable(
        retryFor = BusinessException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000L, multiplier = 2.0)
    )
    public CrowdStatusResponse getCrowdStatus(String areaName) {
        Assert.hasText(areaName, "areaName은 필수입니다");

        log.debug("{} API 호출: areaName={}", API_NAME, areaName);

        try {
            String url = buildCrowdUrl(areaName);
            CrowdStatusResponse response = httpClientUtils.get(url, CrowdStatusResponse.class);
            validateResponse(response);
            return response;
        } catch (ResourceAccessException e) {
            log.error("{} API 타임아웃: {}", API_NAME, e.getMessage());
            throw new BusinessException(ExternalApiErrorCode.API_TIMEOUT, API_NAME + " API 응답 시간 초과");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("{} API 호출 실패: {}", API_NAME, e.getMessage());
            throw new BusinessException(ExternalApiErrorCode.API_CALL_FAILED, API_NAME + " API 호출 실패");
        }
    }

    private String buildCrowdUrl(String areaName) {
        String encodedAreaName = URLEncoder.encode(areaName, StandardCharsets.UTF_8);
        return properties.seoul().baseUrl()
            + "/" + properties.seoul().serviceKey()
            + "/json/citydata/1/5/"
            + encodedAreaName;
    }

    private void validateResponse(CrowdStatusResponse response) {
        if (response == null || response.cityData() == null) {
            throw new BusinessException(ExternalApiErrorCode.API_RESPONSE_PARSE_FAILED,
                API_NAME + " 응답이 비어있습니다");
        }
    }
}
