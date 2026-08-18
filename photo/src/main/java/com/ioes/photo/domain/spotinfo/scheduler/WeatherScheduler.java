package com.ioes.photo.domain.spotinfo.scheduler;

import com.ioes.photo.domain.spotinfo.collector.WeatherCollector;
import com.ioes.photo.domain.spotinfo.service.CollectResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 기상청 단기예보 수집 스케줄러.
 *
 * 기상청 발표 시각(02/05/08/11/14/17/20/23)의 10분 후 구동한다.
 * {@code app.spotinfo.collect.enabled=false} 인 환경에서는 빈이 생성되지 않아 수집하지 않는다.
 *
 * @author 김성민
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.spotinfo.collect.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class WeatherScheduler {

    private final WeatherCollector weatherCollector;

    @Scheduled(cron = "0 10 2,5,8,11,14,17,20,23 * * *")
    public void run() {
        long start = System.currentTimeMillis();
        log.debug("[WeatherScheduler] start");
        try {
            CollectResult result = weatherCollector.collect();
            log.info("[WeatherScheduler] done success={} fail={} total={} duration={}ms",
                result.success(), result.fail(), result.total(),
                System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("[WeatherScheduler] unexpected failure", e);
        }
    }
}
