package com.ioes.photo.external.crowd;

import com.ioes.photo.external.config.properties.ExternalApiProperties;
import com.ioes.photo.external.crowd.dto.TourCrowdRateResponse;
import com.ioes.photo.external.crowd.dto.TourCrowdRateResponse.Body;
import com.ioes.photo.external.crowd.dto.TourCrowdRateResponse.Header;
import com.ioes.photo.external.crowd.dto.TourCrowdRateResponse.Item;
import com.ioes.photo.external.error.ExternalApiErrorCode;
import com.ioes.photo.global.common.util.HttpClientUtils;
import com.ioes.photo.global.error.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 한국관광공사 관광지 집중률 예측 API 클라이언트.
 *
 * <p>대전은 서울과 달리 실시간 혼잡도 API가 없어, KT 통신 데이터 기반의
 * 관광지별 향후 30일 집중률 예측치로 혼잡도를 보완합니다.
 * 시군구 단위로 조회하며 응답의 tAtsNm 은 crowd_areas(category='대전관광지')의
 * area_name 과 동일한 원문입니다.</p>
 *
 * @author 김성민
 * @see TourCrowdRateResponse
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DaejeonCrowdApiClient {

    private static final String API_NAME = "관광지 집중률 예측";
    private static final String CNCTR_RATE_PATH = "/B551011/TatsCnctrRateService/tatsCnctrRatedList";
    private static final String DAEJEON_AREA_CODE = "30";
    private static final String SUCCESS_CODE = "0000";
    // 구별 관광지 수십 곳 × 30일 = 최대 천여 건이라 단일 페이지로 충분하다.
    private static final int NUM_OF_ROWS = 3000;

    /** 대전 5개 자치구 법정동 시군구 코드 (동구/중구/서구/유성구/대덕구). */
    public static final List<String> DAEJEON_SIGNGU_CODES =
        List.of("30110", "30140", "30170", "30200", "30230");

    private final HttpClientUtils httpClientUtils;
    private final ExternalApiProperties properties;

    /**
     * 시군구 내 관광지별 집중률 예측 조회 (오늘부터 30일).
     *
     * <p>외부 API 일시 장애에 대해 최대 3회 재시도(1s → 2s → 4s 백오프)한다.</p>
     *
     * @param signguCd 시군구 코드 ({@link #DAEJEON_SIGNGU_CODES})
     * @return 관광지·날짜별 집중률 목록
     */
    @Retryable(
        retryFor = BusinessException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000L, multiplier = 2.0)
    )
    public List<Item> getCnctrRates(String signguCd) {
        Assert.hasText(signguCd, "signguCd는 필수입니다");

        log.debug("{} API 호출: signguCd={}", API_NAME, signguCd);

        try {
            String url = buildUrl(signguCd);
            TourCrowdRateResponse response = httpClientUtils.get(url, TourCrowdRateResponse.class);
            return extractItems(response, signguCd);
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

    private String buildUrl(String signguCd) {
        return UriComponentsBuilder
            .fromUriString(properties.dataGoKr().baseUrl() + CNCTR_RATE_PATH)
            .queryParam("serviceKey", properties.dataGoKr().serviceKey())
            .queryParam("numOfRows", NUM_OF_ROWS)
            .queryParam("pageNo", 1)
            .queryParam("MobileOS", "ETC")
            .queryParam("MobileApp", "pickflow")
            .queryParam("areaCd", DAEJEON_AREA_CODE)
            .queryParam("signguCd", signguCd)
            .queryParam("_type", "json")
            .build(false)
            .toUriString();
    }

    private List<Item> extractItems(TourCrowdRateResponse response, String signguCd) {
        if (response == null || response.response() == null || response.response().header() == null) {
            throw new BusinessException(ExternalApiErrorCode.API_RESPONSE_PARSE_FAILED,
                API_NAME + " 응답이 비어있습니다");
        }
        // 관광공사 자체 코드 체계("0000")라 공공데이터포털 공통 validator("00")를 쓰지 않는다.
        Header header = response.response().header();
        if (!SUCCESS_CODE.equals(header.resultCode())) {
            log.warn("{} API 오류 응답: code={}, msg={}", API_NAME, header.resultCode(), header.resultMsg());
            throw new BusinessException(ExternalApiErrorCode.API_CALL_FAILED,
                API_NAME + " 오류: " + header.resultMsg());
        }

        Body body = response.response().body();
        if (body == null || body.items() == null || body.items().item() == null) {
            return List.of();
        }
        if (body.totalCount() != null && body.totalCount() > body.items().item().size()) {
            log.warn("{} 응답이 잘렸습니다: signguCd={} totalCount={} received={}",
                API_NAME, signguCd, body.totalCount(), body.items().item().size());
        }
        return body.items().item();
    }
}
