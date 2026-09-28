package com.ioes.photo.domain.spotinfo.collector;

import com.ioes.photo.domain.crowdarea.entity.CrowdArea;
import com.ioes.photo.domain.crowdarea.repository.CrowdAreaRepository;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spotinfo.service.CollectResult;
import com.ioes.photo.domain.spotinfo.service.SpotInfoUpdateService;
import com.ioes.photo.external.crowd.SeoulCrowdApiClient;
import com.ioes.photo.external.crowd.dto.CrowdStatusResponse;
import com.ioes.photo.external.crowd.dto.CrowdStatusResponse.LivePopulation;
import com.ioes.photo.external.crowd.enums.CongestionLevel;
import com.ioes.photo.global.common.util.NullUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 서울시 실시간 혼잡도 수집기.
 *
 * crowd_area_name 이 매핑된 스팟(미공개 포함)을 장소별로 묶어 단일 API 호출로 공유한다.
 * 호출량은 스팟 수와 무관하게 매핑된 장소 수(최대 121곳)로 제한된다.
 * 대전 관광지에 매핑된 스팟은 {@link DaejeonCrowdCollector}가 예측 API 로 수집하므로 제외한다.
 * 장소 단위 실패는 격리된다.
 *
 * @author 김성민
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrowdCollector {

    private static final DateTimeFormatter POPULATION_TIME_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SpotRepository spotRepository;
    private final CrowdAreaRepository crowdAreaRepository;
    private final SpotInfoUpdateService spotInfoUpdateService;
    private final SeoulCrowdApiClient seoulCrowdApiClient;

    public CollectResult collect() {
        Set<String> daejeonAreaNames = crowdAreaRepository
            .findAllByCategory(CrowdArea.CATEGORY_DAEJEON_TOUR).stream()
            .map(CrowdArea::getAreaName)
            .collect(Collectors.toSet());
        Map<String, List<Spot>> grouped = spotRepository
            .findAllByCrowdAreaNameIsNotNull().stream()
            .filter(spot -> !daejeonAreaNames.contains(spot.getCrowdAreaName()))
            .collect(Collectors.groupingBy(Spot::getCrowdAreaName));

        int success = 0;
        int fail = 0;
        for (Map.Entry<String, List<Spot>> entry : grouped.entrySet()) {
            String areaName = entry.getKey();
            List<Spot> spotsInArea = entry.getValue();
            try {
                applyArea(areaName, spotsInArea);
                success += spotsInArea.size();
            } catch (Exception e) {
                log.warn("[CrowdCollector] failed areaName={} size={} reason={}",
                    areaName, spotsInArea.size(), e.getMessage());
                fail += spotsInArea.size();
            }
        }
        return new CollectResult(success, fail);
    }

    private void applyArea(String areaName, List<Spot> spots) {
        CrowdStatusResponse response = seoulCrowdApiClient.getCrowdStatus(areaName);
        LivePopulation live = extractLive(response);
        CongestionLevel level = CongestionLevel.fromLabel(live.congestionLevel());
        Integer populationMin = parseNullableInt(live.populationMin());
        Integer populationMax = parseNullableInt(live.populationMax());
        LocalDateTime observedAt = parseObservedAt(live.populationTime());
        for (Spot spot : spots) {
            spotInfoUpdateService.upsertCrowd(
                spot.getId(),
                level,
                live.congestionMessage(),
                populationMin,
                populationMax,
                observedAt
            );
        }
    }

    private LivePopulation extractLive(CrowdStatusResponse response) {
        List<LivePopulation> stats = response.cityData().livePopulationStats();
        if (stats == null || stats.isEmpty()) {
            throw new IllegalStateException("LIVE_PPLTN_STTS 비어있음");
        }
        return stats.get(0);
    }

    private Integer parseNullableInt(String value) {
        if (NullUtils.isBlank(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.debug("[CrowdCollector] population 숫자 파싱 실패 value={} reason={}",
                value, e.getMessage());
            return null;
        }
    }

    private LocalDateTime parseObservedAt(String populationTime) {
        if (NullUtils.isBlank(populationTime)) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(populationTime.trim(), POPULATION_TIME_FORMAT);
        } catch (Exception e) {
            log.debug("[CrowdCollector] populationTime 파싱 실패 value={} reason={}",
                populationTime, e.getMessage());
            return LocalDateTime.now();
        }
    }
}
