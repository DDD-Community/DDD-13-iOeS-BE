package com.ioes.photo.external.astronomy;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ioes.photo.external.astronomy.dto.SunMoonRiseSetResponse;
import com.ioes.photo.external.config.properties.ExternalApiProperties;
import com.ioes.photo.global.common.util.HttpClientUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AstronomyApiClientTest {

    private final XmlMapper xmlMapper = new XmlMapper();

    @Test
    @DisplayName("요청 URI의 한글 location 파라미터는 1회만 인코딩된다 (이중 인코딩 방지)")
    void encodesLocationExactlyOnce() {
        HttpClientUtils httpClientUtils = mock(HttpClientUtils.class);
        ExternalApiProperties properties = new ExternalApiProperties(
            new ExternalApiProperties.DataGoKr("https://apis.data.go.kr", "test-key"), null);
        AstronomyApiClient client = new AstronomyApiClient(httpClientUtils, properties);
        given(httpClientUtils.get(any(URI.class), eq(SunMoonRiseSetResponse.class)))
            .willReturn(new SunMoonRiseSetResponse(
                new SunMoonRiseSetResponse.Header("00", "NORMAL SERVICE."), null));

        client.getRiseSetInfo("20260607", "서울");

        ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);
        verify(httpClientUtils).get(captor.capture(), eq(SunMoonRiseSetResponse.class));
        assertThat(captor.getValue().getRawQuery()).contains("location=%EC%84%9C%EC%9A%B8");
        assertThat(captor.getValue().toString()).doesNotContain("%25");
    }

    @Test
    @DisplayName("출몰시각 XML 응답을 정상적으로 파싱한다")
    void parseSunMoonRiseSetResponse() throws Exception {
        String xml = new ClassPathResource("external/sun-moon-rise-set-response.xml")
            .getContentAsString(StandardCharsets.UTF_8);

        SunMoonRiseSetResponse response = xmlMapper.readValue(xml, SunMoonRiseSetResponse.class);

        assertThat(response.header().resultCode()).isEqualTo("00");
        assertThat(response.body().totalCount()).isEqualTo(1);

        SunMoonRiseSetResponse.Item item = response.body().items().item().get(0);
        assertThat(item.location()).isEqualTo("서울");
        assertThat(item.locdate()).isEqualTo("20260406");
    }

    @Test
    @DisplayName("출몰시각 응답값의 공백 패딩을 trim 처리한다")
    void trimPaddedValues() throws Exception {
        String xml = new ClassPathResource("external/sun-moon-rise-set-response.xml")
            .getContentAsString(StandardCharsets.UTF_8);

        SunMoonRiseSetResponse response = xmlMapper.readValue(xml, SunMoonRiseSetResponse.class);
        SunMoonRiseSetResponse.Item item = response.body().items().item().get(0);

        assertThat(item.sunrise()).contains(" ");
        assertThat(item.trimmedSunrise()).isEqualTo("0747");
        assertThat(item.trimmedSunset()).isEqualTo("1724");
        assertThat(item.trimmedMoonrise()).isEqualTo("0904");
        assertThat(item.trimmedCivilm()).isEqualTo("0718");
        assertThat(item.trimmedCivile()).isEqualTo("1754");
    }
}
