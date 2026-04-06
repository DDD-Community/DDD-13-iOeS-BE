package com.ioes.photo.external.crowd;

import com.ioes.photo.external.config.properties.ExternalApiProperties;
import com.ioes.photo.external.crowd.dto.CrowdStatusResponse;
import com.ioes.photo.external.error.ExternalApiErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

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

    private final RestClient restClient;
    private final ExternalApiProperties properties;

    /**
     * 서울시 실시간 인구 데이터 조회
     *
     * @param areaName 장소명 (예: "광화문·덕수궁")
     * @return 혼잡도 응답
     */
    public CrowdStatusResponse getCrowdStatus(String areaName) {
        Assert.hasText(areaName, "areaName은 필수입니다");

        log.debug("서울시 혼잡도 API 호출: areaName={}", areaName);

        try {
            String url = buildCrowdUrl(areaName);

            CrowdStatusResponse response = restClient.get()
                .uri(url)
                .retrieve()
                .body(CrowdStatusResponse.class);

            validateResponse(response);
            return response;
        } catch (ResourceAccessException e) {
            log.error("서울시 혼잡도 API 타임아웃: {}", e.getMessage());
            throw new BusinessException(ExternalApiErrorCode.API_TIMEOUT, "서울시 혼잡도 API 응답 시간 초과");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("서울시 혼잡도 API 호출 실패: {}", e.getMessage());
            throw new BusinessException(ExternalApiErrorCode.API_CALL_FAILED, "서울시 혼잡도 API 호출 실패");
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
                "서울시 혼잡도 응답이 비어있습니다");
        }
    }
}
