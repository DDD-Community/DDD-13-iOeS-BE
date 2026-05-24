package com.ioes.photo.external.kakao;

import com.ioes.photo.external.error.ExternalApiErrorCode;
import com.ioes.photo.external.kakao.dto.Coord2AddressResponse;
import com.ioes.photo.external.kakao.dto.KakaoAddress;
import com.ioes.photo.global.common.util.HttpClientUtils;
import com.ioes.photo.global.config.oauth.properties.OAuthProperties;
import com.ioes.photo.global.error.exception.BusinessException;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

/**
 * 카카오 로컬 API 클라이언트.
 *
 * <p>coord2address 엔드포인트로 위·경도를 도로명/지번 주소로 역지오코딩한다.
 * 카카오 REST API 키는 OAuth 설정({@code app.oauth.kakao.client-id})을 재사용한다.</p>
 *
 * @author 김성민
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoLocalApiClient {

    private static final String API_NAME = "카카오 주소";
    private static final String COORD_TO_ADDRESS_URL =
        "https://dapi.kakao.com/v2/local/geo/coord2address.json";

    private final HttpClientUtils httpClientUtils;
    private final OAuthProperties oAuthProperties;

    @Retryable(
        retryFor = BusinessException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000L, multiplier = 2.0)
    )
    public Optional<KakaoAddress> reverseGeocode(double latitude, double longitude) {
        try {
            String url = COORD_TO_ADDRESS_URL + "?x=" + longitude + "&y=" + latitude;
            Coord2AddressResponse response = httpClientUtils.get(url, authHeader(), Coord2AddressResponse.class);
            return KakaoAddress.from(response);
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

    private Consumer<HttpHeaders> authHeader() {
        String restApiKey = oAuthProperties.kakao().clientId();
        return headers -> headers.set(HttpHeaders.AUTHORIZATION, "KakaoAK " + restApiKey);
    }
}
