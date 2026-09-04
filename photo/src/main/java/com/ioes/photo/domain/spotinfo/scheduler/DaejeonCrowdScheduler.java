package com.ioes.photo.domain.spotinfo.scheduler;

import com.ioes.photo.domain.spotinfo.collector.DaejeonCrowdCollector;
import com.ioes.photo.domain.spotinfo.service.CollectResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 대전 혼잡도 수집 스케줄러 (매일 00:40).
 *
 * 집중률 예측 데이터가 일 단위라 하루 1회 수집한다.
 * {@code app.spotinfo.collect.enabled=false} 인 환경에서는 빈이 생성되지 않아 수집하지 않는다.
 *
 * @author 김성민
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.spotinfo.collect.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DaejeonCrowdScheduler {

    private final DaejeonCrowdCollector daejeonCrowdCollector;

    @Scheduled(cron = "0 40 0 * * *")
    public void run() {
        long start = System.currentTimeMillis();
        log.debug("[DaejeonCrowdScheduler] start");
        try {
            CollectResult result = daejeonCrowdCollector.collect();
            log.info("[DaejeonCrowdScheduler] done success={} fail={} total={} duration={}ms",
                result.success(), result.fail(), result.total(),
                System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("[DaejeonCrowdScheduler] unexpected failure", e);
        }
    }
}
