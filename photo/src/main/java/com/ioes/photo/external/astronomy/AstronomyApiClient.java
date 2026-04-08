package com.ioes.photo.external.astronomy;

import com.ioes.photo.external.astronomy.dto.SunMoonRiseSetResponse;
import com.ioes.photo.external.config.properties.ExternalApiProperties;
import com.ioes.photo.external.error.ExternalApiErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

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

    private static final String RISE_SET_PATH =
        "/B090041/openapi/service/RiseSetInfoService/getAreaRiseSetInfo";

    private final RestClient restClient;
    private final ExternalApiProperties properties;

    /**
     * 지역별 해달 출몰시각 정보 조회
     *
     * @param locdate  날짜 (yyyyMMdd)
     * @param location 지역명 (예: 서울, 부산)
     * @return 출몰시각 응답
     */
    public SunMoonRiseSetResponse getRiseSetInfo(String locdate, String location) {
        Assert.hasText(locdate, "locdate는 필수입니다");
        Assert.hasText(location, "location은 필수입니다");

        log.debug("출몰시각 API 호출: locdate={}, location={}", locdate, location);

        try {
            String url = buildRiseSetUrl(locdate, location);

            SunMoonRiseSetResponse response = restClient.get()
                .uri(url)
                .retrieve()
                .body(SunMoonRiseSetResponse.class);

            validateDataGoKrResponse(response);
            return response;
        } catch (ResourceAccessException e) {
            log.error("출몰시각 API 타임아웃: {}", e.getMessage());
            throw new BusinessException(ExternalApiErrorCode.API_TIMEOUT, "출몰시각 API 응답 시간 초과");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("출몰시각 API 호출 실패: {}", e.getMessage());
            throw new BusinessException(ExternalApiErrorCode.API_CALL_FAILED, "출몰시각 API 호출 실패");
        }
    }

    private String buildRiseSetUrl(String locdate, String location) {
        return UriComponentsBuilder
            .fromUriString(properties.dataGoKr().baseUrl() + RISE_SET_PATH)
            .queryParam("serviceKey", properties.dataGoKr().serviceKey())
            .queryParam("locdate", locdate)
            .queryParam("location", location)
            .build(false)
            .encode()
            .toUriString();
    }

    private void validateDataGoKrResponse(SunMoonRiseSetResponse response) {
        if (response == null || response.header() == null) {
            throw new BusinessException(ExternalApiErrorCode.API_RESPONSE_PARSE_FAILED,
                "출몰시각 응답이 비어있습니다");
        }

        String resultCode = response.header().resultCode();
        if ("00".equals(resultCode)) {
            return;
        }

        log.warn("출몰시각 API 오류 응답: code={}, msg={}", resultCode, response.header().resultMsg());

        if ("30".equals(resultCode)) {
            throw new BusinessException(ExternalApiErrorCode.API_RATE_LIMIT_EXCEEDED);
        }
        if ("31".equals(resultCode) || "32".equals(resultCode)) {
            throw new BusinessException(ExternalApiErrorCode.API_SERVICE_KEY_INVALID);
        }

        throw new BusinessException(ExternalApiErrorCode.API_CALL_FAILED,
            "출몰시각 오류: " + response.header().resultMsg());
    }
}
