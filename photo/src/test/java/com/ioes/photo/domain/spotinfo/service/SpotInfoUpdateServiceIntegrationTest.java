package com.ioes.photo.domain.spotinfo.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ioes.photo.domain.spotinfo.entity.SpotInfo;
import com.ioes.photo.domain.spotinfo.repository.SpotInfoRepository;
import com.ioes.photo.external.crowd.enums.CongestionLevel;
import com.ioes.photo.external.weather.enums.PrecipitationType;
import com.ioes.photo.external.weather.enums.SkyStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * SpotInfoUpdateService 통합 테스트.
 *
 * <p>실제 JPA + H2 로 upsert 동작을 검증한다.</p>
 * <ul>
 *   <li>신규 spotId 로 호출 시 row 생성</li>
 *   <li>기존 row 갱신 시 다른 영역 필드가 보존되는지 (영역별 분리 저장)</li>
 *   <li>연속 호출로 전 영역이 누적되는지</li>
 * </ul>
 *
 * @author 김성민
 */
@SpringBootTest
@DisplayName("SpotInfoUpdateService 통합 테스트 — 실제 DB upsert 동작")
class SpotInfoUpdateServiceIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () ->
            "c2VjcmV0LWtleS1mb3ItdGVzdGluZy1wdXJwb3Nlcy1vbmx5LW11c3QtYmUtYXQtbGVhc3QtNjQtYnl0ZXMtbG9uZw==");
    }

    @MockitoBean RedisConnectionFactory redisConnectionFactory;
    @MockitoBean ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @Autowired SpotInfoUpdateService spotInfoUpdateService;
    @Autowired SpotInfoRepository spotInfoRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM spot_info");
    }

    @Test
    @DisplayName("신규 spotId로 upsertCrowd 호출 시 SpotInfo row 가 생성된다")
    void createNewRowOnFirstCrowdUpsert() {
        Long spotId = 100L;
        LocalDateTime observedAt = LocalDateTime.of(2026, 4, 20, 14, 0);

        spotInfoUpdateService.upsertCrowd(spotId, CongestionLevel.NORMAL,
            "평소와 비슷함", 10000, 12000, observedAt);

        SpotInfo saved = spotInfoRepository.findById(spotId).orElseThrow();
        assertThat(saved.getSpotId()).isEqualTo(spotId);
        assertThat(saved.getCongestionLevel()).isEqualTo(CongestionLevel.NORMAL);
        assertThat(saved.getPopulationMin()).isEqualTo(10000);
        assertThat(saved.getPopulationMax()).isEqualTo(12000);
        assertThat(saved.getCongestionUpdatedAt()).isEqualTo(observedAt);
        assertThat(saved.getWeatherSky()).isNull();
        assertThat(saved.getSunriseTime()).isNull();
    }

    @Test
    @DisplayName("기존 row 에 upsertWeather 호출 시 혼잡도 필드는 보존되고 날씨만 갱신된다")
    void weatherUpsertPreservesCrowdFields() {
        Long spotId = 200L;
        LocalDateTime crowdAt = LocalDateTime.of(2026, 4, 20, 14, 0);
        LocalDateTime weatherAt = LocalDateTime.of(2026, 4, 20, 15, 0);

        spotInfoUpdateService.upsertCrowd(spotId, CongestionLevel.CROWDED,
            "붐빔", 50000, 60000, crowdAt);
        spotInfoUpdateService.upsertWeather(spotId, SkyStatus.CLEAR,
            PrecipitationType.NONE, 10, 23.5, weatherAt);

        SpotInfo saved = spotInfoRepository.findById(spotId).orElseThrow();
        assertThat(saved.getCongestionLevel()).isEqualTo(CongestionLevel.CROWDED);
        assertThat(saved.getPopulationMin()).isEqualTo(50000);
        assertThat(saved.getPopulationMax()).isEqualTo(60000);
        assertThat(saved.getCongestionUpdatedAt()).isEqualTo(crowdAt);
        assertThat(saved.getWeatherSky()).isEqualTo(SkyStatus.CLEAR);
        assertThat(saved.getWeatherPrecipitation()).isEqualTo(PrecipitationType.NONE);
        assertThat(saved.getPrecipitationProbability()).isEqualTo(10);
        assertThat(saved.getTemperature()).isEqualTo(23.5);
        assertThat(saved.getWeatherUpdatedAt()).isEqualTo(weatherAt);
    }

    @Test
    @DisplayName("혼잡도 → 날씨 → 천문 순으로 upsert 시 3개 영역이 모두 누적된다")
    void allThreeAreasAccumulate() {
        Long spotId = 300L;
        LocalDateTime crowdAt = LocalDateTime.of(2026, 4, 20, 14, 0);
        LocalDateTime weatherAt = LocalDateTime.of(2026, 4, 20, 15, 0);
        LocalDate astronomyDate = LocalDate.of(2026, 4, 20);

        spotInfoUpdateService.upsertCrowd(spotId, CongestionLevel.RELAXED,
            "여유", 5000, 7000, crowdAt);
        spotInfoUpdateService.upsertWeather(spotId, SkyStatus.OVERCAST,
            PrecipitationType.RAIN, 80, 18.0, weatherAt);
        spotInfoUpdateService.upsertAstronomy(spotId, astronomyDate,
            LocalTime.of(5, 45), LocalTime.of(18, 50));

        SpotInfo saved = spotInfoRepository.findById(spotId).orElseThrow();
        assertThat(saved.getCongestionLevel()).isEqualTo(CongestionLevel.RELAXED);
        assertThat(saved.getWeatherSky()).isEqualTo(SkyStatus.OVERCAST);
        assertThat(saved.getAstronomyDate()).isEqualTo(astronomyDate);
        assertThat(saved.getSunriseTime()).isEqualTo(LocalTime.of(5, 45));
        assertThat(saved.getSunsetTime()).isEqualTo(LocalTime.of(18, 50));
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("동일 영역 재호출 시 최신 값으로 덮어쓰인다")
    void sameAreaOverwritesPreviousValues() {
        Long spotId = 400L;
        LocalDateTime first = LocalDateTime.of(2026, 4, 20, 14, 0);
        LocalDateTime second = LocalDateTime.of(2026, 4, 20, 14, 10);

        spotInfoUpdateService.upsertCrowd(spotId, CongestionLevel.RELAXED,
            "여유", 5000, 7000, first);
        spotInfoUpdateService.upsertCrowd(spotId, CongestionLevel.CROWDED,
            "붐빔", 40000, 50000, second);

        SpotInfo saved = spotInfoRepository.findById(spotId).orElseThrow();
        assertThat(saved.getCongestionLevel()).isEqualTo(CongestionLevel.CROWDED);
        assertThat(saved.getPopulationMin()).isEqualTo(40000);
        assertThat(saved.getPopulationMax()).isEqualTo(50000);
        assertThat(saved.getCongestionUpdatedAt()).isEqualTo(second);
    }
}
