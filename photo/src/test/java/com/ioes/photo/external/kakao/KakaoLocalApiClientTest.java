package com.ioes.photo.external.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.ioes.photo.external.error.ExternalApiErrorCode;
import com.ioes.photo.external.kakao.dto.Coord2AddressResponse;
import com.ioes.photo.external.kakao.dto.Coord2AddressResponse.Address;
import com.ioes.photo.external.kakao.dto.Coord2AddressResponse.Document;
import com.ioes.photo.external.kakao.dto.Coord2AddressResponse.RoadAddress;
import com.ioes.photo.external.kakao.dto.KakaoAddress;
import com.ioes.photo.global.common.util.HttpClientUtils;
import com.ioes.photo.global.config.oauth.properties.OAuthProperties;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

/**
 * {@link KakaoLocalApiClient} 단위 테스트.
 *
 * @author 김성민
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KakaoLocalApiClient 단위 테스트")
class KakaoLocalApiClientTest {

    @Mock HttpClientUtils httpClientUtils;
    @Mock OAuthProperties oAuthProperties;

    @InjectMocks KakaoLocalApiClient kakaoLocalApiClient;

    private void givenRestApiKey() {
        given(oAuthProperties.kakao())
            .willReturn(new OAuthProperties.Kakao("REST_API_KEY", null));
    }

    @Test
    @DisplayName("정상 응답이면 도로명/지번 주소를 반환한다")
    void returnsAddress_whenSuccess() {
        givenRestApiKey();
        Coord2AddressResponse response = new Coord2AddressResponse(List.of(
            new Document(
                new RoadAddress("서울특별시 영등포구 여의공원로 68", "서울특별시", "영등포구"),
                new Address("서울특별시 영등포구 여의도동 18", "서울특별시", "영등포구")
            )
        ));
        given(httpClientUtils.get(anyString(), any(Consumer.class), eq(Coord2AddressResponse.class)))
            .willReturn(response);

        Optional<KakaoAddress> result = kakaoLocalApiClient.reverseGeocode(37.5326, 126.9905);

        assertThat(result).isPresent();
        assertThat(result.get().roadAddress()).isEqualTo("서울특별시 영등포구 여의공원로 68");
        assertThat(result.get().jibunAddress()).isEqualTo("서울특별시 영등포구 여의도동 18");
    }

    @Test
    @DisplayName("타임아웃이면 API_TIMEOUT 예외로 변환한다")
    void throwsApiTimeout_whenResourceAccessException() {
        givenRestApiKey();
        given(httpClientUtils.get(anyString(), any(Consumer.class), eq(Coord2AddressResponse.class)))
            .willThrow(new ResourceAccessException("timeout"));

        assertThatThrownBy(() -> kakaoLocalApiClient.reverseGeocode(37.5326, 126.9905))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ExternalApiErrorCode.API_TIMEOUT);
    }

    @Test
    @DisplayName("기타 예외면 API_CALL_FAILED로 변환한다")
    void throwsApiCallFailed_whenUnexpectedException() {
        givenRestApiKey();
        given(httpClientUtils.get(anyString(), any(Consumer.class), eq(Coord2AddressResponse.class)))
            .willThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> kakaoLocalApiClient.reverseGeocode(37.5326, 126.9905))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ExternalApiErrorCode.API_CALL_FAILED);
    }

    @Test
    @DisplayName("BusinessException은 그대로 전파한다")
    void propagatesBusinessException() {
        givenRestApiKey();
        given(httpClientUtils.get(anyString(), any(Consumer.class), eq(Coord2AddressResponse.class)))
            .willThrow(new BusinessException(CommonErrorCode.UNAUTHORIZED));

        assertThatThrownBy(() -> kakaoLocalApiClient.reverseGeocode(37.5326, 126.9905))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }
}
