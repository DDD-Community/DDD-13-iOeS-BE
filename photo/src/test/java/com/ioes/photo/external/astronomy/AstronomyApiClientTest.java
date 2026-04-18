package com.ioes.photo.external.astronomy;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ioes.photo.external.astronomy.dto.SunMoonRiseSetResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AstronomyApiClientTest {

    private final XmlMapper xmlMapper = new XmlMapper();

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
