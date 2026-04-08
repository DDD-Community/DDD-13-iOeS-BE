package com.ioes.photo.external.weather;

import com.ioes.photo.external.config.properties.ExternalApiProperties;
import com.ioes.photo.external.error.ExternalApiErrorCode;
import com.ioes.photo.external.weather.dto.ShortTermForecastResponse;
import com.ioes.photo.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 기상청 단기예보 API 클라이언트.
 *
 * <p>공공데이터포털의 단기예보 조회서비스를 호출하여
 * 기온, 강수확률, 하늘상태, 풍향/풍속 등의 예보 데이터를 조회합니다.</p>
 *
 * @author 김성민
 * @see ShortTermForecastResponse
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherApiClient {

    private static final String SHORT_TERM_FORECAST_PATH =
        "/1360000/VilageFcstInfoService_2.0/getVilageFcst";
    private static final int DEFAULT_NUM_OF_ROWS = 1000;

    private final RestClient restClient;
    private final ExternalApiProperties properties;

    /**
     * 단기예보 조회
     *
     * @param baseDate 발표일자 (yyyyMMdd)
     * @param baseTime 발표시각 (HHmm, 예: 0200, 0500, 0800 ...)
     * @param nx       예보지점 X좌표
     * @param ny       예보지점 Y좌표
     * @return 단기예보 응답
     */
    public ShortTermForecastResponse getShortTermForecast(
        String baseDate, String baseTime, int nx, int ny
    ) {
        Assert.hasText(baseDate, "baseDate는 필수입니다");
        Assert.hasText(baseTime, "baseTime은 필수입니다");

        String url = buildForecastUrl(baseDate, baseTime, nx, ny);
        log.debug("단기예보 API 호출: baseDate={}, baseTime={}, nx={}, ny={}", baseDate, baseTime, nx, ny);

        try {
            ShortTermForecastResponse response = restClient.get()
                .uri(url)
                .retrieve()
                .body(ShortTermForecastResponse.class);

            validateDataGoKrResponse(response);
            return response;
        } catch (ResourceAccessException e) {
            log.error("단기예보 API 타임아웃: {}", e.getMessage());
            throw new BusinessException(ExternalApiErrorCode.API_TIMEOUT, "기상청 단기예보 API 응답 시간 초과");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("단기예보 API 호출 실패: {}", e.getMessage());
            throw new BusinessException(ExternalApiErrorCode.API_CALL_FAILED, "기상청 단기예보 API 호출 실패");
        }
    }

    private String buildForecastUrl(String baseDate, String baseTime, int nx, int ny) {
        return UriComponentsBuilder
            .fromUriString(properties.dataGoKr().baseUrl() + SHORT_TERM_FORECAST_PATH)
            .queryParam("serviceKey", properties.dataGoKr().serviceKey())
            .queryParam("numOfRows", DEFAULT_NUM_OF_ROWS)
            .queryParam("pageNo", 1)
            .queryParam("dataType", "XML")
            .queryParam("base_date", baseDate)
            .queryParam("base_time", baseTime)
            .queryParam("nx", nx)
            .queryParam("ny", ny)
            .build(false)
            .toUriString();
    }

    private void validateDataGoKrResponse(ShortTermForecastResponse response) {
        if (response == null || response.header() == null) {
            throw new BusinessException(ExternalApiErrorCode.API_RESPONSE_PARSE_FAILED,
                "기상청 단기예보 응답이 비어있습니다");
        }

        String resultCode = response.header().resultCode();
        if ("00".equals(resultCode)) {
            return;
        }

        log.warn("단기예보 API 오류 응답: code={}, msg={}", resultCode, response.header().resultMsg());

        if ("30".equals(resultCode)) {
            throw new BusinessException(ExternalApiErrorCode.API_RATE_LIMIT_EXCEEDED);
        }
        if ("31".equals(resultCode) || "32".equals(resultCode)) {
            throw new BusinessException(ExternalApiErrorCode.API_SERVICE_KEY_INVALID);
        }

        throw new BusinessException(ExternalApiErrorCode.API_CALL_FAILED,
            "기상청 단기예보 오류: " + response.header().resultMsg());
    }
}
