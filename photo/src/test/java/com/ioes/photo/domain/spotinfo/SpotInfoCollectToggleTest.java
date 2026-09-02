package com.ioes.photo.domain.spotinfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.ioes.photo.domain.crowdarea.service.CrowdAreaMapper;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spotinfo.collector.AstronomyCollector;
import com.ioes.photo.domain.spotinfo.collector.CrowdCollector;
import com.ioes.photo.domain.spotinfo.collector.DaejeonCrowdCollector;
import com.ioes.photo.domain.spotinfo.collector.WeatherCollector;
import com.ioes.photo.domain.spotinfo.scheduler.AstronomyScheduler;
import com.ioes.photo.domain.spotinfo.scheduler.CrowdScheduler;
import com.ioes.photo.domain.spotinfo.scheduler.DaejeonCrowdScheduler;
import com.ioes.photo.domain.spotinfo.scheduler.WeatherScheduler;
import com.ioes.photo.domain.spotinfo.startup.SpotInfoBootstrap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 수집 스위치 {@code app.spotinfo.collect.enabled} 동작 검증.
 *
 * 프로퍼티명이 네 클래스에 문자열로 반복되므로, 한 곳만 오타가 나도 해당 빈은 스위치를 무시하고
 * 계속 수집한다. 운영과 외부 API 서비스키를 공유하는 환경에서는 이 누락이 일일 트래픽 한도 소진으로
 * 이어지므로, 네 빈이 스위치에 함께 반응하는지 확인한다.
 *
 * @author 김성민
 */
@DisplayName("스팟 부가정보 수집 스위치")
class SpotInfoCollectToggleTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withBean(WeatherCollector.class, () -> mock(WeatherCollector.class))
        .withBean(AstronomyCollector.class, () -> mock(AstronomyCollector.class))
        .withBean(CrowdCollector.class, () -> mock(CrowdCollector.class))
        .withBean(DaejeonCrowdCollector.class, () -> mock(DaejeonCrowdCollector.class))
        .withBean(SpotRepository.class, () -> mock(SpotRepository.class))
        .withBean(CrowdAreaMapper.class, () -> mock(CrowdAreaMapper.class))
        .withUserConfiguration(CollectComponents.class);

    @Test
    @DisplayName("미설정이면 수집 빈이 모두 등록된다")
    void registersAllBeansWhenPropertyMissing() {
        runner.run(context -> assertThat(context)
            .hasSingleBean(WeatherScheduler.class)
            .hasSingleBean(AstronomyScheduler.class)
            .hasSingleBean(CrowdScheduler.class)
            .hasSingleBean(DaejeonCrowdScheduler.class)
            .hasSingleBean(SpotInfoBootstrap.class));
    }

    @Test
    @DisplayName("false 면 수집 빈이 모두 등록되지 않는다")
    void registersNoBeansWhenDisabled() {
        runner.withPropertyValues("app.spotinfo.collect.enabled=false")
            .run(context -> assertThat(context)
                .doesNotHaveBean(WeatherScheduler.class)
                .doesNotHaveBean(AstronomyScheduler.class)
                .doesNotHaveBean(CrowdScheduler.class)
                .doesNotHaveBean(DaejeonCrowdScheduler.class)
                .doesNotHaveBean(SpotInfoBootstrap.class));
    }

    @Configuration(proxyBeanMethods = false)
    @Import({WeatherScheduler.class, AstronomyScheduler.class, CrowdScheduler.class,
        DaejeonCrowdScheduler.class, SpotInfoBootstrap.class})
    static class CollectComponents {
    }
}
