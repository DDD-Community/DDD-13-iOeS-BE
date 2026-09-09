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

import com.ioes.photo.domain.crowdarea.entity.CrowdArea;
import com.ioes.photo.domain.crowdarea.repository.CrowdAreaRepository;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spotinfo.service.CollectResult;
import com.ioes.photo.domain.spotinfo.service.SpotInfoUpdateService;
import com.ioes.photo.external.crowd.SeoulCrowdApiClient;
import com.ioes.photo.external.crowd.dto.CrowdStatusResponse;
import com.ioes.photo.external.crowd.dto.CrowdStatusResponse.CityData;
import com.ioes.photo.external.crowd.dto.CrowdStatusResponse.LivePopulation;
import com.ioes.photo.external.crowd.enums.CongestionLevel;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrowdCollectorTest {

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private CrowdAreaRepository crowdAreaRepository;

    @Mock
    private SpotInfoUpdateService spotInfoUpdateService;

    @Mock
    private SeoulCrowdApiClient seoulCrowdApiClient;

    @InjectMocks
    private CrowdCollector crowdCollector;

    @Test
    @DisplayName("모든 스팟 수집 성공 시 success 카운트가 일치한다")
    void allSuccess() {
        Spot a = mockSpot(1L, "광화문·덕수궁");
        Spot b = mockSpot(2L, "강남역");
        given(spotRepository.findAllByStatusAndCrowdAreaNameIsNotNull(SpotStatus.PUBLISHED))
            .willReturn(List.of(a, b));
        given(seoulCrowdApiClient.getCrowdStatus(anyString()))
            .willReturn(crowdResponse("보통"));

        CollectResult result = crowdCollector.collect();

        assertThat(result.success()).isEqualTo(2);
        assertThat(result.fail()).isZero();
        verify(spotInfoUpdateService, times(2)).upsertCrowd(
            any(), eq(CongestionLevel.NORMAL), any(), any(), any(), any());
    }

    @Test
    @DisplayName("개별 스팟 API 실패는 격리되어 다음 스팟 처리를 막지 않는다")
    void isolatesPerSpotFailure() {
        Spot a = mockSpot(1L, "광화문·덕수궁");
        Spot b = mockSpot(2L, "실패장소");
        Spot c = mockSpot(3L, "강남역");
        given(spotRepository.findAllByStatusAndCrowdAreaNameIsNotNull(SpotStatus.PUBLISHED))
            .willReturn(List.of(a, b, c));
        given(seoulCrowdApiClient.getCrowdStatus("광화문·덕수궁"))
            .willReturn(crowdResponse("여유"));
        given(seoulCrowdApiClient.getCrowdStatus("실패장소"))
            .willThrow(new RuntimeException("boom"));
        given(seoulCrowdApiClient.getCrowdStatus("강남역"))
            .willReturn(crowdResponse("붐빔"));

        CollectResult result = crowdCollector.collect();

        assertThat(result.success()).isEqualTo(2);
        assertThat(result.fail()).isEqualTo(1);
        verify(spotInfoUpdateService, times(2)).upsertCrowd(
            any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("대상 스팟이 없으면 API를 호출하지 않는다")
    void skipsWhenNoTargets() {
        given(spotRepository.findAllByStatusAndCrowdAreaNameIsNotNull(SpotStatus.PUBLISHED))
            .willReturn(List.of());

        CollectResult result = crowdCollector.collect();

        assertThat(result.total()).isZero();
        verify(seoulCrowdApiClient, never()).getCrowdStatus(anyString());
        verify(spotInfoUpdateService, never())
            .upsertCrowd(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("대전 관광지에 매핑된 스팟은 서울 수집 대상에서 제외된다")
    void excludesDaejeonSpots() {
        CrowdArea daejeonArea = mock(CrowdArea.class);
        given(daejeonArea.getAreaName()).willReturn("유성온천지구");
        given(crowdAreaRepository.findAllByCategory(CrowdArea.CATEGORY_DAEJEON_TOUR))
            .willReturn(List.of(daejeonArea));
        Spot seoul = mockSpot(1L, "광화문·덕수궁");
        Spot daejeon = mock(Spot.class);
        given(daejeon.getCrowdAreaName()).willReturn("유성온천지구");
        given(spotRepository.findAllByStatusAndCrowdAreaNameIsNotNull(SpotStatus.PUBLISHED))
            .willReturn(List.of(seoul, daejeon));
        given(seoulCrowdApiClient.getCrowdStatus("광화문·덕수궁"))
            .willReturn(crowdResponse("보통"));

        CollectResult result = crowdCollector.collect();

        assertThat(result.total()).isEqualTo(1);
        verify(seoulCrowdApiClient, never()).getCrowdStatus("유성온천지구");
    }

    private Spot mockSpot(long id, String areaName) {
        Spot spot = mock(Spot.class);
        given(spot.getId()).willReturn(id);
        given(spot.getCrowdAreaName()).willReturn(areaName);
        return spot;
    }

    private CrowdStatusResponse crowdResponse(String congestionLabel) {
        LivePopulation pop = new LivePopulation(
            "AREA", "CD", congestionLabel, "평소 비슷함",
            "10000", "12000",
            "50.0", "50.0",
            "0", "0", "0", "0", "0", "0", "0", "0",
            "2026-04-19 14:00", "N", List.of()
        );
        return new CrowdStatusResponse(new CityData("AREA", "CD", List.of(pop)));
    }
}
