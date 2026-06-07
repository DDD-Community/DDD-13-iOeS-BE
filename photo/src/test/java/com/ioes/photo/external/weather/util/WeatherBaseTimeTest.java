package com.ioes.photo.external.weather.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.ioes.photo.external.weather.util.WeatherBaseTime.BaseInfo;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WeatherBaseTimeTest {

    @Test
    @DisplayName("발표 시각 직후 10분은 아직 이전 발표를 반환한다")
    void justAfterPublishWithinDelay() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 19, 14, 5);

        BaseInfo base = WeatherBaseTime.resolve(now);

        assertThat(base.baseDate()).isEqualTo("20260419");
        assertThat(base.baseTime()).isEqualTo("1100");
    }

    @Test
    @DisplayName("발표 10분 후는 해당 발표를 반환한다")
    void exactlyAfterDelay() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 19, 14, 15);

        BaseInfo base = WeatherBaseTime.resolve(now);

        assertThat(base.baseDate()).isEqualTo("20260419");
        assertThat(base.baseTime()).isEqualTo("1400");
    }

    @Test
    @DisplayName("첫 발표(02시) 전 자정 직후는 전날 23시를 반환한다")
    void beforeFirstPublish() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 19, 1, 30);

        BaseInfo base = WeatherBaseTime.resolve(now);

        assertThat(base.baseDate()).isEqualTo("20260418");
        assertThat(base.baseTime()).isEqualTo("2300");
    }

    @Test
    @DisplayName("23시 발표 + 10분 후는 당일 23시를 반환한다")
    void afterLastPublish() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 19, 23, 30);

        BaseInfo base = WeatherBaseTime.resolve(now);

        assertThat(base.baseDate()).isEqualTo("20260419");
        assertThat(base.baseTime()).isEqualTo("2300");
    }
}
