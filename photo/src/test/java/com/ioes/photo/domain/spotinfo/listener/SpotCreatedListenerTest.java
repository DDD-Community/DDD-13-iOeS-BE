package com.ioes.photo.domain.spotinfo.listener;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.event.SpotCreatedEvent;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spotinfo.collector.AstronomyCollector;
import com.ioes.photo.domain.spotinfo.collector.WeatherCollector;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpotCreatedListenerTest {

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private WeatherCollector weatherCollector;

    @Mock
    private AstronomyCollector astronomyCollector;

    @InjectMocks
    private SpotCreatedListener spotCreatedListener;

    @Test
    @DisplayName("스팟 생성 이벤트 수신 시 날씨/천문 정보를 즉시 수집한다")
    void collectsWeatherAndAstronomyOnSpotCreated() {
        Spot spot = mockSpot(1L);
        given(spotRepository.findById(1L)).willReturn(Optional.of(spot));

        spotCreatedListener.onSpotCreated(new SpotCreatedEvent(1L));

        verify(weatherCollector).collectForSpot(spot);
        verify(astronomyCollector).collectForSpot(1L);
    }

    @Test
    @DisplayName("날씨 수집 실패가 천문 수집을 막지 않는다")
    void astronomyStillCollectedWhenWeatherFails() {
        Spot spot = mockSpot(1L);
        given(spotRepository.findById(1L)).willReturn(Optional.of(spot));
        willThrow(new RuntimeException("api down")).given(weatherCollector).collectForSpot(spot);

        spotCreatedListener.onSpotCreated(new SpotCreatedEvent(1L));

        verify(astronomyCollector).collectForSpot(1L);
    }

    @Test
    @DisplayName("스팟이 없으면 수집하지 않는다")
    void skipsWhenSpotNotFound() {
        given(spotRepository.findById(1L)).willReturn(Optional.empty());

        spotCreatedListener.onSpotCreated(new SpotCreatedEvent(1L));

        verifyNoInteractions(weatherCollector, astronomyCollector);
    }

    private Spot mockSpot(long id) {
        Spot spot = mock(Spot.class);
        given(spot.getId()).willReturn(id);
        return spot;
    }
}
