package com.ioes.photo.domain.spotinfo.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spotinfo.service.CollectResult;
import com.ioes.photo.domain.spotinfo.service.SpotInfoUpdateService;
import com.ioes.photo.external.weather.WeatherApiClient;
import com.ioes.photo.external.weather.config.WeatherProperties;
import com.ioes.photo.external.weather.dto.ShortTermForecastResponse;
import com.ioes.photo.external.weather.dto.ShortTermForecastResponse.Body;
import com.ioes.photo.external.weather.dto.ShortTermForecastResponse.Header;
import com.ioes.photo.external.weather.dto.ShortTermForecastResponse.Item;
import com.ioes.photo.external.weather.dto.ShortTermForecastResponse.Items;
import com.ioes.photo.external.weather.enums.SkyStatus;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeatherCollectorTest {

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private SpotInfoUpdateService spotInfoUpdateService;

    @Mock
    private WeatherApiClient weatherApiClient;

    private WeatherCollector weatherCollector;

    @BeforeEach
    void setUp() {
        weatherCollector = new WeatherCollector(
            spotRepository,
            spotInfoUpdateService,
            weatherApiClient,
            new WeatherProperties(10)
        );
    }

    @Test
    @DisplayName("동일 격자 스팟은 단일 API 호출로 처리된다")
    void groupsByGridAndCallsOnce() {
        Spot a = mockSpot(1L, 60, 127);
        Spot b = mockSpot(2L, 60, 127);
        Spot c = mockSpot(3L, 61, 125);
        given(spotRepository.findAllByStatusAndGridNxIsNotNullAndGridNyIsNotNull(SpotStatus.PUBLISHED))
            .willReturn(List.of(a, b, c));
        given(weatherApiClient.getShortTermForecast(anyString(), anyString(), anyInt(), anyInt()))
            .willReturn(forecastResponse());

        CollectResult result = weatherCollector.collect();

        assertThat(result.success()).isEqualTo(3);
        verify(weatherApiClient, times(2))
            .getShortTermForecast(anyString(), anyString(), anyInt(), anyInt());
        verify(weatherApiClient).getShortTermForecast(anyString(), anyString(), eq(60), eq(127));
        verify(weatherApiClient).getShortTermForecast(anyString(), anyString(), eq(61), eq(125));
        verify(spotInfoUpdateService, times(3)).upsertWeather(
            any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("격자 단위 API 실패 시 해당 격자 내 스팟이 fail로 집계된다")
    void failedGridMarksAllSpotsAsFail() {
        Spot a = mockSpot(1L, 60, 127);
        Spot b = mockSpot(2L, 60, 127);
        Spot c = mockSpot(3L, 61, 125);
        given(spotRepository.findAllByStatusAndGridNxIsNotNullAndGridNyIsNotNull(SpotStatus.PUBLISHED))
            .willReturn(List.of(a, b, c));
        given(weatherApiClient.getShortTermForecast(anyString(), anyString(), eq(60), eq(127)))
            .willThrow(new RuntimeException("grid failed"));
        given(weatherApiClient.getShortTermForecast(anyString(), anyString(), eq(61), eq(125)))
            .willReturn(forecastResponse());

        CollectResult result = weatherCollector.collect();

        assertThat(result.fail()).isEqualTo(2);
        assertThat(result.success()).isEqualTo(1);
    }

    private Spot mockSpot(long id, int nx, int ny) {
        Spot spot = mock(Spot.class);
        lenient().when(spot.getId()).thenReturn(id);
        when(spot.getGridNx()).thenReturn(nx);
        when(spot.getGridNy()).thenReturn(ny);
        return spot;
    }

    private ShortTermForecastResponse forecastResponse() {
        LocalDateTime target = LocalDateTime.now().plusHours(1).withMinute(0);
        String fcstDate = target.toLocalDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String fcstTime = String.format("%02d00", target.getHour());
        List<Item> items = List.of(
            new Item("20260419", "1400", "SKY", fcstDate, fcstTime, SkyStatus.CLEAR.getCode(), 60, 127),
            new Item("20260419", "1400", "PTY", fcstDate, fcstTime, "0", 60, 127),
            new Item("20260419", "1400", "TMP", fcstDate, fcstTime, "23", 60, 127)
        );
        return new ShortTermForecastResponse(
            new Header("00", "NORMAL_SERVICE"),
            new Body("XML", new Items(items), 3, 1, 3)
        );
    }
}
