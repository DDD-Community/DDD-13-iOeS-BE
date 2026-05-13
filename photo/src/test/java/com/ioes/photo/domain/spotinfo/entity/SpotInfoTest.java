package com.ioes.photo.domain.spotinfo.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.ioes.photo.external.crowd.enums.CongestionLevel;
import com.ioes.photo.external.weather.enums.PrecipitationType;
import com.ioes.photo.external.weather.enums.SkyStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SpotInfoTest {

    @Test
    @DisplayName("spotId로 신규 SpotInfo를 생성한다")
    void create() {
        SpotInfo info = SpotInfo.create(42L);

        assertThat(info.getSpotId()).isEqualTo(42L);
        assertThat(info.getCongestionLevel()).isNull();
        assertThat(info.getSunriseTime()).isNull();
    }

    @Test
    @DisplayName("혼잡도 업데이트 시 혼잡도 필드와 시각만 변경된다")
    void updateCrowdOnlyAffectsCrowdFields() {
        SpotInfo info = SpotInfo.create(1L);
        LocalDateTime observedAt = LocalDateTime.of(2026, 4, 19, 14, 0);

        info.updateCrowd(CongestionLevel.NORMAL, "인구가 평소와 비교하여 비슷함",
            10000, 12000, observedAt);

        assertThat(info.getCongestionLevel()).isEqualTo(CongestionLevel.NORMAL);
        assertThat(info.getCongestionMessage()).isEqualTo("인구가 평소와 비교하여 비슷함");
        assertThat(info.getPopulationMin()).isEqualTo(10000);
        assertThat(info.getPopulationMax()).isEqualTo(12000);
        assertThat(info.getCongestionUpdatedAt()).isEqualTo(observedAt);
        assertThat(info.getWeatherSky()).isNull();
        assertThat(info.getSunriseTime()).isNull();
    }

    @Test
    @DisplayName("날씨 업데이트는 혼잡도 필드를 건드리지 않는다")
    void updateWeatherKeepsCrowdIntact() {
        SpotInfo info = SpotInfo.create(1L);
        info.updateCrowd(CongestionLevel.CROWDED, "붐빔", 50000, 60000,
            LocalDateTime.of(2026, 4, 19, 14, 0));

        info.updateWeather(SkyStatus.CLEAR, PrecipitationType.NONE, 30, 23.5,
            LocalDateTime.of(2026, 4, 19, 15, 0));

        assertThat(info.getCongestionLevel()).isEqualTo(CongestionLevel.CROWDED);
        assertThat(info.getWeatherSky()).isEqualTo(SkyStatus.CLEAR);
        assertThat(info.getWeatherPrecipitation()).isEqualTo(PrecipitationType.NONE);
        assertThat(info.getPrecipitationProbability()).isEqualTo(30);
        assertThat(info.getTemperature()).isEqualTo(23.5);
    }

    @Test
    @DisplayName("천문 업데이트는 혼잡도/날씨 필드를 건드리지 않는다")
    void updateAstronomyKeepsOthersIntact() {
        SpotInfo info = SpotInfo.create(1L);
        info.updateWeather(SkyStatus.OVERCAST, PrecipitationType.RAIN, 70, 18.0,
            LocalDateTime.of(2026, 4, 19, 15, 0));

        info.updateAstronomy(LocalDate.of(2026, 4, 19),
            LocalTime.of(5, 45), LocalTime.of(18, 50));

        assertThat(info.getAstronomyDate()).isEqualTo(LocalDate.of(2026, 4, 19));
        assertThat(info.getSunriseTime()).isEqualTo(LocalTime.of(5, 45));
        assertThat(info.getSunsetTime()).isEqualTo(LocalTime.of(18, 50));
        assertThat(info.getWeatherSky()).isEqualTo(SkyStatus.OVERCAST);
    }
}
