package com.ioes.photo.domain.spotinfo.scheduler;

import com.ioes.photo.domain.spotinfo.collector.CrowdCollector;
import com.ioes.photo.domain.spotinfo.service.CollectResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 혼잡도 수집 스케줄러 (10분 주기).
 *
 * @author 김성민
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrowdScheduler {

    private static final long TEN_MINUTES_MS = 10L * 60 * 1000;
    private static final long ONE_MINUTE_MS = 60L * 1000;

    private final CrowdCollector crowdCollector;

    @Scheduled(fixedDelay = TEN_MINUTES_MS, initialDelay = ONE_MINUTE_MS)
    public void run() {
        long start = System.currentTimeMillis();
        log.debug("[CrowdScheduler] start");
        try {
            CollectResult result = crowdCollector.collect();
            log.info("[CrowdScheduler] done success={} fail={} total={} duration={}ms",
                result.success(), result.fail(), result.total(),
                System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("[CrowdScheduler] unexpected failure", e);
        }
    }
}
