package com.ioes.photo.domain.statistics.scheduler;

import com.ioes.photo.domain.statistics.dto.StatisticsSnapshot;
import com.ioes.photo.domain.statistics.notion.NotionProperties;
import com.ioes.photo.domain.statistics.notion.NotionStatisticsSync;
import com.ioes.photo.domain.statistics.service.StatisticsAggregationService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 운영 지표 노션 동기화 스케줄러 (매일 06:00, 어제 기준).
 *
 * @author 김성민
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsSyncScheduler {

    private final StatisticsAggregationService aggregationService;
    private final NotionStatisticsSync notionStatisticsSync;
    private final NotionProperties notionProperties;

    @Scheduled(cron = "0 0 6 * * *")
    public void run() {
        if (!notionProperties.isConfigured()) {
            log.warn("[StatisticsSyncScheduler] 노션 설정(token/database-id) 미구성으로 스킵");
            return;
        }

        LocalDate target = LocalDate.now().minusDays(1);
        try {
            StatisticsSnapshot snapshot = aggregationService.aggregate(target);
            notionStatisticsSync.upsert(snapshot);
            log.info("[StatisticsSyncScheduler] done date={}", target);
        } catch (Exception e) {
            log.error("[StatisticsSyncScheduler] failed date={}", target, e);
        }
    }
}
