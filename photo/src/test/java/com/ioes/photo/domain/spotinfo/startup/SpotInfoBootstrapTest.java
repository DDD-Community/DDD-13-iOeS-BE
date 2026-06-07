package com.ioes.photo.domain.spotinfo.startup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ioes.photo.domain.crowdarea.service.CrowdAreaMapper;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spotinfo.collector.AstronomyCollector;
import com.ioes.photo.domain.spotinfo.collector.WeatherCollector;
import com.ioes.photo.domain.spotinfo.service.CollectResult;
import com.ioes.photo.external.weather.util.LccGridConverter;
import com.ioes.photo.external.weather.util.LccGridConverter.GridPoint;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpotInfoBootstrapTest {

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private CrowdAreaMapper crowdAreaMapper;

    @Mock
    private WeatherCollector weatherCollector;

    @Mock
    private AstronomyCollector astronomyCollector;

    @InjectMocks
    private SpotInfoBootstrap spotInfoBootstrap;

    @Test
    @DisplayName("격자 좌표 없는 스팟에 위경도 기반 격자와 혼잡도 지역을 백필한다")
    void backfillsGridAndCrowdArea() {
        Spot spot = spotWithoutGrid(37.5326, 126.9905);
        given(spotRepository.findAllByGridNxIsNullOrGridNyIsNull()).willReturn(List.of(spot));
        given(crowdAreaMapper.findNearestAreaName(37.5326, 126.9905))
            .willReturn(Optional.of("여의도한강공원"));
        givenCollectorsSucceed();

        spotInfoBootstrap.run();

        GridPoint expected = LccGridConverter.toGrid(37.5326, 126.9905);
        assertThat(spot.getGridNx()).isEqualTo(expected.nx());
        assertThat(spot.getGridNy()).isEqualTo(expected.ny());
        assertThat(spot.getCrowdAreaName()).isEqualTo("여의도한강공원");
        then(spotRepository).should().saveAll(List.of(spot));
    }

    @Test
    @DisplayName("백필 대상이 없으면 저장 없이 수집만 수행한다")
    void skipsSaveWhenNothingToBackfill() {
        given(spotRepository.findAllByGridNxIsNullOrGridNyIsNull()).willReturn(List.of());
        givenCollectorsSucceed();

        spotInfoBootstrap.run();

        then(spotRepository).should(never()).saveAll(anyList());
        verify(weatherCollector).collect();
        verify(astronomyCollector).collect();
    }

    @Test
    @DisplayName("날씨 수집 실패가 천문 수집을 막지 않는다")
    void astronomyStillCollectedWhenWeatherFails() {
        given(spotRepository.findAllByGridNxIsNullOrGridNyIsNull()).willReturn(List.of());
        given(weatherCollector.collect()).willThrow(new RuntimeException("api down"));
        given(astronomyCollector.collect()).willReturn(new CollectResult(1, 0));

        spotInfoBootstrap.run();

        verify(astronomyCollector).collect();
    }

    @Test
    @DisplayName("혼잡도 지역이 이미 있으면 격자만 백필한다")
    void keepsExistingCrowdAreaName() {
        Spot spot = Spot.builder()
            .name("스팟")
            .theme(SpotTheme.SUNSET)
            .latitude(37.5326)
            .longitude(126.9905)
            .crowdAreaName("기존지역")
            .build();
        given(spotRepository.findAllByGridNxIsNullOrGridNyIsNull()).willReturn(List.of(spot));
        givenCollectorsSucceed();

        spotInfoBootstrap.run();

        assertThat(spot.getGridNx()).isNotNull();
        assertThat(spot.getCrowdAreaName()).isEqualTo("기존지역");
        then(crowdAreaMapper).should(never()).findNearestAreaName(anyDouble(), anyDouble());
    }

    private void givenCollectorsSucceed() {
        given(weatherCollector.collect()).willReturn(new CollectResult(1, 0));
        given(astronomyCollector.collect()).willReturn(new CollectResult(1, 0));
    }

    private Spot spotWithoutGrid(double latitude, double longitude) {
        return Spot.builder()
            .name("스팟")
            .theme(SpotTheme.SUNSET)
            .latitude(latitude)
            .longitude(longitude)
            .build();
    }
}
