package com.ioes.photo.external.crowd;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ioes.photo.external.crowd.dto.TourCrowdRateResponse;
import com.ioes.photo.external.crowd.enums.CongestionLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DaejeonCrowdApiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    @DisplayName("관광지 집중률 JSON 응답을 정상적으로 파싱한다")
    void parseTourCrowdRateResponse() throws Exception {
        String json = new ClassPathResource("external/tour-crowd-rate-response.json")
            .getContentAsString(StandardCharsets.UTF_8);

        TourCrowdRateResponse response = objectMapper.readValue(json, TourCrowdRateResponse.class);

        assertThat(response.response().header().resultCode()).isEqualTo("0000");
        assertThat(response.response().body().totalCount()).isEqualTo(2);

        TourCrowdRateResponse.Item item = response.response().body().items().item().get(0);
        assertThat(item.baseYmd()).isEqualTo("20260902");
        assertThat(item.signguNm()).isEqualTo("서구");
        assertThat(item.tAtsNm()).isEqualTo("구봉산(대전)");
        assertThat(item.cnctrRate()).isEqualTo("26.62");
    }

    @Test
    @DisplayName("집중률 구간 경계값이 합의된 4단계 기준대로 변환된다")
    void convertRateToCongestionLevel() {
        assertThat(CongestionLevel.fromRate(0)).isEqualTo(CongestionLevel.RELAXED);
        assertThat(CongestionLevel.fromRate(29.99)).isEqualTo(CongestionLevel.RELAXED);
        assertThat(CongestionLevel.fromRate(30)).isEqualTo(CongestionLevel.NORMAL);
        assertThat(CongestionLevel.fromRate(44.99)).isEqualTo(CongestionLevel.NORMAL);
        assertThat(CongestionLevel.fromRate(45)).isEqualTo(CongestionLevel.SLIGHTLY_CROWDED);
        assertThat(CongestionLevel.fromRate(69.99)).isEqualTo(CongestionLevel.SLIGHTLY_CROWDED);
        assertThat(CongestionLevel.fromRate(70)).isEqualTo(CongestionLevel.CROWDED);
        assertThat(CongestionLevel.fromRate(100)).isEqualTo(CongestionLevel.CROWDED);
    }
}
