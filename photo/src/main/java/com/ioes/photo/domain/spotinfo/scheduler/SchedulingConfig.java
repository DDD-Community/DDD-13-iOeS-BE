package com.ioes.photo.domain.spotinfo.scheduler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 스케줄러 구성.
 *
 * 3개의 수집 스케줄러(혼잡도/날씨/천문)가 동시 실행될 수 있고,
 * 한 작업이 외부 API 지연 등으로 길어졌을 때 다음 트리거가 큐잉되어
 * 지연되지 않도록 여유분 2를 더해 풀 크기 5로 운영한다.
 *
 * @author 김성민
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("spot-info-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}
