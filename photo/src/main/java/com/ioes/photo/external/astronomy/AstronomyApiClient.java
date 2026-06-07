package com.ioes.photo.external.astronomy;

import com.ioes.photo.external.astronomy.dto.SunMoonRiseSetResponse;
import com.ioes.photo.external.common.DataGoKrResponseValidator;
import com.ioes.photo.external.config.properties.ExternalApiProperties;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * 한국천문연구원 출몰시각 API 클라이언트.
 *
 * <p>공공데이터포털의 출몰시각 정보 서비스를 호출하여
 * 일출/일몰, 월출/월몰, 박명 시각 데이터를 조회합니다.</p>
 *
 * @author 김성민
 * @see SunMoonRiseSetResponse
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AstronomyApiClient {

    private static final String API_NAME = "출몰시각";
    private static final String RISE_SET_PATH =
        "/B090041/openapi/service/RiseSetInfoService/getAreaRiseSetInfo";

    private final HttpClientUtils httpClientUtils;
    private final ExternalApiProperties properties;

    /**
     * 지역별 해달 출몰시각 정보 조회.
     *
     * <p>외부 API 일시 장애에 대해 최대 3회 재시도(1s → 2s → 4s 백오프)한다.</p>
     *
     * @param locdate  날짜 (yyyyMMdd)
     * @param location 지역명 (예: 서울, 부산)
     * @return 출몰시각 응답
     */
    @Retryable(
        retryFor = BusinessException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000L, multiplier = 2.0)
    )
    public SunMoonRiseSetResponse getRiseSetInfo(String locdate, String location) {
        Assert.hasText(locdate, "locdate는 필수입니다");
        Assert.hasText(location, "location은 필수입니다");

        log.debug("출몰시각 API 호출: locdate={}, location={}", locdate, location);

        try {
            URI uri = buildRiseSetUri(locdate, location);
            SunMoonRiseSetResponse response = httpClientUtils.get(uri, SunMoonRiseSetResponse.class);
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

    private URI buildRiseSetUri(String locdate, String location) {
        return UriComponentsBuilder
            .fromUriString(properties.dataGoKr().baseUrl() + RISE_SET_PATH)
            .queryParam("serviceKey", properties.dataGoKr().serviceKey())
            .queryParam("locdate", locdate)
            .queryParam("location", location)
            .build(false)
            .encode()
            .toUri();
    }

    private void validateResponse(SunMoonRiseSetResponse response) {
        if (response == null || response.header() == null) {
            throw new BusinessException(ExternalApiErrorCode.API_RESPONSE_PARSE_FAILED,
                API_NAME + " 응답이 비어있습니다");
        }
        DataGoKrResponseValidator.validate(
            response.header().resultCode(),
            response.header().resultMsg(),
            API_NAME
        );
    }
}
