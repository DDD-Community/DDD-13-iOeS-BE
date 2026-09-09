package com.ioes.photo.domain.spotinfo.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ioes.photo.domain.crowdarea.entity.CrowdArea;
import com.ioes.photo.domain.crowdarea.repository.CrowdAreaRepository;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spotinfo.service.CollectResult;
import com.ioes.photo.domain.spotinfo.service.SpotInfoUpdateService;
import com.ioes.photo.external.crowd.DaejeonCrowdApiClient;
import com.ioes.photo.external.crowd.dto.TourCrowdRateResponse.Item;
import com.ioes.photo.external.crowd.enums.CongestionLevel;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DaejeonCrowdCollectorTest {

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private CrowdAreaRepository crowdAreaRepository;

    @Mock
    private SpotInfoUpdateService spotInfoUpdateService;

    @Mock
    private DaejeonCrowdApiClient daejeonCrowdApiClient;

    @InjectMocks
    private DaejeonCrowdCollector collector;

    @Test
    @DisplayName("대전 관광지에 매핑된 스팟만 집중률로 혼잡도를 저장한다")
    void collectsOnlyDaejeonSpots() {
        givenDaejeonAreas("유성온천지구");
        Spot daejeon = mockSpot(1L, "유성온천지구");
        Spot seoul = mockSpot(2L, "광화문·덕수궁");
        given(spotRepository.findAllByStatusAndCrowdAreaNameIsNotNull(SpotStatus.PUBLISHED))
            .willReturn(List.of(daejeon, seoul));
        givenRates(item("유성온천지구", "20260902", "84.95"));

        CollectResult result = collector.collect();

        assertThat(result.success()).isEqualTo(1);
        assertThat(result.fail()).isZero();
        verify(spotInfoUpdateService).upsertCrowd(
            eq(1L), eq(CongestionLevel.CROWDED), anyString(), isNull(), isNull(), any());
        verify(spotInfoUpdateService, never()).upsertCrowd(
            eq(2L), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("여러 예측일 중 가장 가까운 날짜의 집중률을 사용한다")
    void usesNearestBaseYmd() {
        givenDaejeonAreas("유성온천지구");
        Spot daejeon = mockSpot(1L, "유성온천지구");
        given(spotRepository.findAllByStatusAndCrowdAreaNameIsNotNull(SpotStatus.PUBLISHED))
            .willReturn(List.of(daejeon));
        givenRates(
            item("유성온천지구", "20260903", "90.00"),
            item("유성온천지구", "20260902", "20.00")
        );

        collector.collect();

        verify(spotInfoUpdateService).upsertCrowd(
            eq(1L), eq(CongestionLevel.RELAXED), anyString(), isNull(), isNull(), any());
    }

    @Test
    @DisplayName("응답에 관광지명이 없으면 fail로 집계한다")
    void countsFailWhenRateMissing() {
        givenDaejeonAreas("유성온천지구", "노고산");
        Spot matched = mockSpot(1L, "유성온천지구");
        Spot unmatched = mockSpot(2L, "노고산");
        given(spotRepository.findAllByStatusAndCrowdAreaNameIsNotNull(SpotStatus.PUBLISHED))
            .willReturn(List.of(matched, unmatched));
        givenRates(item("유성온천지구", "20260902", "40.00"));

        CollectResult result = collector.collect();

        assertThat(result.success()).isEqualTo(1);
        assertThat(result.fail()).isEqualTo(1);
    }

    @Test
    @DisplayName("시군구 단위 API 실패는 격리되어 다른 구 스팟 처리를 막지 않는다")
    void isolatesPerSignguFailure() {
        givenDaejeonAreas("유성온천지구");
        Spot daejeon = mockSpot(1L, "유성온천지구");
        given(spotRepository.findAllByStatusAndCrowdAreaNameIsNotNull(SpotStatus.PUBLISHED))
            .willReturn(List.of(daejeon));
        // anyString 스텁이 던지는 상태에서 given()으로 재스터빙하면 스텁이 실행되므로 willReturn 선행형을 쓴다.
        given(daejeonCrowdApiClient.getCnctrRates(anyString()))
            .willThrow(new RuntimeException("boom"));
        willReturn(List.of(item("유성온천지구", "20260902", "50.00")))
            .given(daejeonCrowdApiClient).getCnctrRates("30200");

        CollectResult result = collector.collect();

        assertThat(result.success()).isEqualTo(1);
        verify(spotInfoUpdateService).upsertCrowd(
            eq(1L), eq(CongestionLevel.SLIGHTLY_CROWDED), anyString(), isNull(), isNull(), any());
    }

    @Test
    @DisplayName("대상 스팟이 없으면 API를 호출하지 않는다")
    void skipsWhenNoTargets() {
        givenDaejeonAreas("유성온천지구");
        Spot seoul = mockSpot(1L, "광화문·덕수궁");
        given(spotRepository.findAllByStatusAndCrowdAreaNameIsNotNull(SpotStatus.PUBLISHED))
            .willReturn(List.of(seoul));

        CollectResult result = collector.collect();

        assertThat(result.total()).isZero();
        verify(daejeonCrowdApiClient, never()).getCnctrRates(anyString());
        verify(spotInfoUpdateService, never())
            .upsertCrowd(any(), any(), any(), any(), any(), any());
    }

    private void givenDaejeonAreas(String... areaNames) {
        List<CrowdArea> areas = List.of(areaNames).stream()
            .map(name -> {
                CrowdArea area = mock(CrowdArea.class);
                given(area.getAreaName()).willReturn(name);
                return area;
            })
            .toList();
        given(crowdAreaRepository.findAllByCategory(CrowdArea.CATEGORY_DAEJEON_TOUR))
            .willReturn(areas);
    }

    private void givenRates(Item... items) {
        given(daejeonCrowdApiClient.getCnctrRates(anyString())).willReturn(List.of());
        given(daejeonCrowdApiClient.getCnctrRates("30200")).willReturn(List.of(items));
    }

    private Spot mockSpot(long id, String areaName) {
        Spot spot = mock(Spot.class);
        // 서울 스팟은 필터링되어 getId()가 호출되지 않으므로 lenient로 둔다.
        lenient().when(spot.getId()).thenReturn(id);
        given(spot.getCrowdAreaName()).willReturn(areaName);
        return spot;
    }

    private Item item(String tAtsNm, String baseYmd, String cnctrRate) {
        return new Item(baseYmd, "30", "대전광역시", "30200", "유성구", tAtsNm, cnctrRate);
    }
}
