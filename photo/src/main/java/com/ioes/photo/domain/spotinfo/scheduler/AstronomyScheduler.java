package com.ioes.photo.domain.spotinfo.scheduler;

import com.ioes.photo.domain.spotinfo.collector.AstronomyCollector;
import com.ioes.photo.domain.spotinfo.service.CollectResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 천문연구원 출몰시각 수집 스케줄러 (매일 00:10).
 *
 * @author 김성민
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AstronomyScheduler {

    private final AstronomyCollector astronomyCollector;

    @Scheduled(cron = "0 10 0 * * *")
    public void run() {
        long start = System.currentTimeMillis();
        log.info("[AstronomyScheduler] start");
        try {
            CollectResult result = astronomyCollector.collect();
            log.info("[AstronomyScheduler] done success={} fail={} total={} duration={}ms",
                result.success(), result.fail(), result.total(),
                System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("[AstronomyScheduler] unexpected failure", e);
        }
    }
}
