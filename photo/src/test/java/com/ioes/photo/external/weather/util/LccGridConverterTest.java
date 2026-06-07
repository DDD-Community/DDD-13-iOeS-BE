package com.ioes.photo.external.weather.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.ioes.photo.external.weather.util.LccGridConverter.GridPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LccGridConverterTest {

    @Test
    @DisplayName("서울시청 좌표를 기상청 격자로 변환한다")
    void convertSeoul() {
        GridPoint grid = LccGridConverter.toGrid(37.5665, 126.9780);

        assertThat(grid.nx()).isEqualTo(60);
        assertThat(grid.ny()).isEqualTo(127);
    }

    @Test
    @DisplayName("제주 좌표를 기상청 격자로 변환한다")
    void convertJeju() {
        GridPoint grid = LccGridConverter.toGrid(33.4996, 126.5312);

        assertThat(grid.nx()).isEqualTo(53);
        assertThat(grid.ny()).isEqualTo(38);
    }

    @Test
    @DisplayName("속초 좌표를 기상청 격자로 변환한다")
    void convertSokcho() {
        GridPoint grid = LccGridConverter.toGrid(38.2070, 128.5918);

        assertThat(grid.nx()).isEqualTo(87);
        assertThat(grid.ny()).isEqualTo(141);
    }
}
