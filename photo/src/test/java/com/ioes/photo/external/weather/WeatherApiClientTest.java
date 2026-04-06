package com.ioes.photo.external.weather;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ioes.photo.external.weather.dto.ShortTermForecastResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherApiClientTest {

    private final XmlMapper xmlMapper = new XmlMapper();

    @Test
    @DisplayName("단기예보 XML 응답을 정상적으로 파싱한다")
    void parseShortTermForecastResponse() throws Exception {
        String xml = new ClassPathResource("external/short-term-forecast-response.xml")
            .getContentAsString(StandardCharsets.UTF_8);

        ShortTermForecastResponse response = xmlMapper.readValue(xml, ShortTermForecastResponse.class);

        assertThat(response.header().resultCode()).isEqualTo("00");
        assertThat(response.body().totalCount()).isEqualTo(3);
        assertThat(response.body().items().item()).hasSize(3);

        ShortTermForecastResponse.Item tempItem = response.body().items().item().get(0);
        assertThat(tempItem.category()).isEqualTo("TMP");
        assertThat(tempItem.fcstValue()).isEqualTo("8");
        assertThat(tempItem.nx()).isEqualTo(60);
        assertThat(tempItem.ny()).isEqualTo(127);
    }

    @Test
    @DisplayName("에러 응답 코드를 올바르게 판별한다")
    void parseErrorResponse() throws Exception {
        String errorXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <response>
                <header>
                    <resultCode>30</resultCode>
                    <resultMsg>SERVICE_KEY_IS_NOT_REGISTERED_ERROR</resultMsg>
                </header>
                <body/>
            </response>
            """;

        ShortTermForecastResponse response = xmlMapper.readValue(errorXml, ShortTermForecastResponse.class);

        assertThat(response.header().resultCode()).isEqualTo("30");
        assertThat(response.header().resultMsg()).contains("SERVICE_KEY");
    }
}
