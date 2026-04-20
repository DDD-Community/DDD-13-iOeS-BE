package com.ioes.photo.domain.spotinfo.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spotinfo.service.CollectResult;
import com.ioes.photo.domain.spotinfo.service.SpotInfoUpdateService;
import com.ioes.photo.external.astronomy.AstronomyApiClient;
import com.ioes.photo.external.astronomy.dto.SunMoonRiseSetResponse;
import com.ioes.photo.external.astronomy.dto.SunMoonRiseSetResponse.Body;
import com.ioes.photo.external.astronomy.dto.SunMoonRiseSetResponse.Header;
import com.ioes.photo.external.astronomy.dto.SunMoonRiseSetResponse.Item;
import com.ioes.photo.external.astronomy.dto.SunMoonRiseSetResponse.Items;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AstronomyCollectorTest {

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private SpotInfoUpdateService spotInfoUpdateService;

    @Mock
    private AstronomyApiClient astronomyApiClient;

    @InjectMocks
    private AstronomyCollector astronomyCollector;

    @Test
    @DisplayName("서울 기준 1회 호출 후 모든 PUBLISHED 스팟에 동일 값을 적용한다")
    void broadcastsSingleCallToAllSpots() {
        Spot a = mockSpot(1L);
        Spot b = mockSpot(2L);
        Spot c = mockSpot(3L);
        given(astronomyApiClient.getRiseSetInfo(anyString(), eq("서울")))
            .willReturn(riseSetResponse("0545", "1850"));
        given(spotRepository.findAllByStatus(SpotStatus.PUBLISHED))
            .willReturn(List.of(a, b, c));

        CollectResult result = astronomyCollector.collect();

        assertThat(result.success()).isEqualTo(3);
        verify(astronomyApiClient, times(1)).getRiseSetInfo(anyString(), eq("서울"));
        verify(spotInfoUpdateService, times(3)).upsertAstronomy(
            any(), any(), eq(LocalTime.of(5, 45)), eq(LocalTime.of(18, 50)));
    }

    @Test
    @DisplayName("API 호출 실패 시 DB 업데이트는 시도하지 않는다")
    void skipsUpdatesOnApiFailure() {
        given(astronomyApiClient.getRiseSetInfo(anyString(), eq("서울")))
            .willThrow(new RuntimeException("api down"));

        CollectResult result = astronomyCollector.collect();

        assertThat(result.total()).isZero();
        verify(spotInfoUpdateService, never())
            .upsertAstronomy(any(), any(), any(), any());
    }

    private Spot mockSpot(long id) {
        Spot spot = mock(Spot.class);
        given(spot.getId()).willReturn(id);
        return spot;
    }

    private SunMoonRiseSetResponse riseSetResponse(String sunrise, String sunset) {
        Item item = new Item(
            "20260419", "서울", "126-58", "0", "37-33", "0",
            sunrise, "1213", sunset,
            "1820", "0100", "0400",
            "0515", "1920", "0440", "1955", "0400", "2035"
        );
        return new SunMoonRiseSetResponse(
            new Header("00", "NORMAL_SERVICE"),
            new Body(new Items(List.of(item)), 1, 1, 1)
        );
    }
}
