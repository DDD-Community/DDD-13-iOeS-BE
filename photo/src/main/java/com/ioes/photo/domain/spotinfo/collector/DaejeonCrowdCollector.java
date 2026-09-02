package com.ioes.photo.domain.spotinfo.collector;

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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 대전 관광지 집중률 기반 혼잡도 수집기.
 *
 * 대전 관광지(crowd_areas category='대전관광지')에 매핑된 PUBLISHED 스팟을 대상으로,
 * 시군구별 벌크 조회(총 5회) 후 관광지명으로 매칭해 혼잡도 스냅샷을 저장한다.
 * 예측 데이터가 일 단위라 서울(10분)과 달리 하루 1회면 충분하다.
 * 시군구 단위 API 실패는 격리된다(해당 구 관광지만 매칭 실패로 집계).
 *
 * @author 김성민
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DaejeonCrowdCollector {

    // 대전은 실시간이 아닌 예측 지수 기반이라 출처를 문구로 남긴다.
    private static final String PREDICTION_MESSAGE = "관광지 방문자 추이 예측 기반";

    private final SpotRepository spotRepository;
    private final CrowdAreaRepository crowdAreaRepository;
    private final SpotInfoUpdateService spotInfoUpdateService;
    private final DaejeonCrowdApiClient daejeonCrowdApiClient;

    public CollectResult collect() {
        Set<String> daejeonAreaNames = daejeonAreaNames();
        List<Spot> targets = spotRepository
            .findAllByStatusAndCrowdAreaNameIsNotNull(SpotStatus.PUBLISHED).stream()
            .filter(spot -> daejeonAreaNames.contains(spot.getCrowdAreaName()))
            .toList();
        if (targets.isEmpty()) {
            return new CollectResult(0, 0);
        }

        Map<String, Double> ratesByAreaName = fetchNearestDayRates();
        LocalDateTime observedAt = LocalDateTime.now();
        int success = 0;
        int fail = 0;
        for (Spot spot : targets) {
            Double rate = ratesByAreaName.get(spot.getCrowdAreaName());
            if (rate == null) {
                log.warn("[DaejeonCrowdCollector] 집중률 없음 spotId={} areaName={}",
                    spot.getId(), spot.getCrowdAreaName());
                fail++;
                continue;
            }
            try {
                spotInfoUpdateService.upsertCrowd(
                    spot.getId(),
                    CongestionLevel.fromRate(rate),
                    PREDICTION_MESSAGE,
                    null,
                    null,
                    observedAt
                );
                success++;
            } catch (Exception e) {
                log.warn("[DaejeonCrowdCollector] failed spotId={} areaName={} reason={}",
                    spot.getId(), spot.getCrowdAreaName(), e.getMessage());
                fail++;
            }
        }
        return new CollectResult(success, fail);
    }

    private Set<String> daejeonAreaNames() {
        return crowdAreaRepository.findAllByCategory(CrowdArea.CATEGORY_DAEJEON_TOUR).stream()
            .map(CrowdArea::getAreaName)
            .collect(Collectors.toSet());
    }

    /**
     * 관광지명 → 가장 가까운 예측일의 집중률.
     *
     * 응답은 오늘부터 30일치라 관광지별 최소 baseYmd 를 취한다.
     * 날짜 동등 비교 대신 최소값을 쓰는 이유는 서버 타임존과 API 기준일(KST)의
     * 경계 차이로 '오늘' 데이터가 없을 수 있어서다.
     */
    private Map<String, Double> fetchNearestDayRates() {
        Map<String, Item> nearest = new HashMap<>();
        for (String signguCd : DaejeonCrowdApiClient.DAEJEON_SIGNGU_CODES) {
            try {
                for (Item item : daejeonCrowdApiClient.getCnctrRates(signguCd)) {
                    nearest.merge(item.tAtsNm(), item,
                        (a, b) -> a.baseYmd().compareTo(b.baseYmd()) <= 0 ? a : b);
                }
            } catch (Exception e) {
                log.warn("[DaejeonCrowdCollector] 시군구 조회 실패 signguCd={} reason={}",
                    signguCd, e.getMessage());
            }
        }

        Map<String, Double> rates = new HashMap<>();
        for (Item item : nearest.values()) {
            try {
                rates.put(item.tAtsNm(), Double.parseDouble(item.cnctrRate().trim()));
            } catch (RuntimeException e) {
                log.debug("[DaejeonCrowdCollector] cnctrRate 파싱 실패 tAtsNm={} value={}",
                    item.tAtsNm(), item.cnctrRate());
            }
        }
        return rates;
    }
}
