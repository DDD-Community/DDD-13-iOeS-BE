package com.ioes.photo.external.crowd;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ioes.photo.external.crowd.dto.CrowdStatusResponse;
import com.ioes.photo.external.crowd.enums.CongestionLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SeoulCrowdApiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    @DisplayName("서울시 혼잡도 JSON 응답을 정상적으로 파싱한다")
    void parseCrowdStatusResponse() throws Exception {
        String json = new ClassPathResource("external/crowd-status-response.json")
            .getContentAsString(StandardCharsets.UTF_8);

        CrowdStatusResponse response = objectMapper.readValue(json, CrowdStatusResponse.class);

        assertThat(response.cityData().areaNm()).isEqualTo("광화문·덕수궁");
        assertThat(response.cityData().areaCd()).isEqualTo("POI009");
        assertThat(response.cityData().livePopulationStats()).hasSize(1);

        CrowdStatusResponse.LivePopulation pop = response.cityData().livePopulationStats().get(0);
        assertThat(pop.congestionLevel()).isEqualTo("여유");
        assertThat(pop.populationMin()).isEqualTo("12000");
        assertThat(pop.populationMax()).isEqualTo("14000");
        assertThat(pop.maleRate()).isEqualTo("46.8");
    }

    @Test
    @DisplayName("혼잡도 레벨을 enum으로 변환한다")
    void convertCongestionLevel() throws Exception {
        String json = new ClassPathResource("external/crowd-status-response.json")
            .getContentAsString(StandardCharsets.UTF_8);

        CrowdStatusResponse response = objectMapper.readValue(json, CrowdStatusResponse.class);
        String levelLabel = response.cityData().livePopulationStats().get(0).congestionLevel();

        CongestionLevel level = CongestionLevel.fromLabel(levelLabel);
        assertThat(level).isEqualTo(CongestionLevel.RELAXED);
    }

    @Test
    @DisplayName("인구 예측 데이터를 정상적으로 파싱한다")
    void parseForecastPopulation() throws Exception {
        String json = new ClassPathResource("external/crowd-status-response.json")
            .getContentAsString(StandardCharsets.UTF_8);

        CrowdStatusResponse response = objectMapper.readValue(json, CrowdStatusResponse.class);
        var forecasts = response.cityData().livePopulationStats().get(0).forecastPopulations();

        assertThat(forecasts).hasSize(1);
        assertThat(forecasts.get(0).forecastTime()).isEqualTo("2026-04-06 16:00");
        assertThat(forecasts.get(0).congestionLevel()).isEqualTo("보통");
    }
}
