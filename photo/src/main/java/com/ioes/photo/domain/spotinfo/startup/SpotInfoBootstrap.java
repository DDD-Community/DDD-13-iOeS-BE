package com.ioes.photo.domain.spotinfo.startup;

import com.ioes.photo.domain.crowdarea.service.CrowdAreaMapper;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spotinfo.collector.AstronomyCollector;
import com.ioes.photo.domain.spotinfo.collector.WeatherCollector;
import com.ioes.photo.domain.spotinfo.service.CollectResult;
import com.ioes.photo.external.weather.util.LccGridConverter;
import com.ioes.photo.external.weather.util.LccGridConverter.GridPoint;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 기동 시 스팟 수집 메타데이터 백필 및 초기 수집.
 *
 * 격자 좌표 없이 등록된 기존 스팟에 위경도 기반 격자/혼잡도 지역을 채우고,
 * 다음 스케줄 주기를 기다리지 않도록 날씨/천문 수집을 1회 수행한다.
 * 배포 직후 정보 공백이 즉시 복구되며, 모든 단계는 멱등하다.
 *
 * @author 김성민
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpotInfoBootstrap {

    private final SpotRepository spotRepository;
    private final CrowdAreaMapper crowdAreaMapper;
    private final WeatherCollector weatherCollector;
    private final AstronomyCollector astronomyCollector;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        backfillCollectTargets();
        collectOnce();
    }

    private void backfillCollectTargets() {
        List<Spot> targets = spotRepository.findAllByGridNxIsNullOrGridNyIsNull();
        if (targets.isEmpty()) {
            return;
        }
        for (Spot spot : targets) {
            GridPoint grid = LccGridConverter.toGrid(spot.getLatitude(), spot.getLongitude());
            spot.assignGrid(grid.nx(), grid.ny());
            if (spot.getCrowdAreaName() == null) {
                crowdAreaMapper.findNearestAreaName(spot.getLatitude(), spot.getLongitude())
                    .ifPresent(spot::assignCrowdAreaName);
            }
        }
        spotRepository.saveAll(targets);
        log.info("[SpotInfoBootstrap] 격자 좌표 백필 완료 count={}", targets.size());
    }

    private void collectOnce() {
        try {
            CollectResult weather = weatherCollector.collect();
            log.info("[SpotInfoBootstrap] 날씨 초기 수집 success={} fail={}",
                weather.success(), weather.fail());
        } catch (Exception e) {
            log.error("[SpotInfoBootstrap] 날씨 초기 수집 실패", e);
        }
        try {
            CollectResult astronomy = astronomyCollector.collect();
            log.info("[SpotInfoBootstrap] 천문 초기 수집 success={} fail={}",
                astronomy.success(), astronomy.fail());
        } catch (Exception e) {
            log.error("[SpotInfoBootstrap] 천문 초기 수집 실패", e);
        }
    }
}
