package com.ioes.photo.domain.statistics.service;

import com.ioes.photo.domain.statistics.dto.StatisticsSnapshot;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.global.storage.S3StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link StatisticsAggregationService} 통합 테스트 — 실제 MyBatis + H2로 지표 집계 검증.
 *
 * @author 김성민
 */
@SpringBootTest
@DisplayName("StatisticsAggregationService 통합 테스트")
class StatisticsAggregationServiceIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () ->
            "c2VjcmV0LWtleS1mb3ItdGVzdGluZy1wdXJwb3Nlcy1vbmx5LW11c3QtYmUtYXQtbGVhc3QtNjQtYnl0ZXMtbG9uZw==");
    }

    @MockitoBean RedisConnectionFactory redisConnectionFactory;
    @MockitoBean ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;
    @MockitoBean S3StorageService storageService;

    @Autowired StatisticsAggregationService metricsAggregationService;
    @Autowired SpotRepository spotRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private static final LocalDate TARGET = LocalDate.now().minusDays(1);

    @BeforeEach
    void clean() {
        // @SpringBootTest 공유 컨텍스트에서 다른 테스트가 남긴 데이터로부터 격리하기 위해
        // 시작 시점에도 대상 테이블을 비운다.
        clearTables();
    }

    @AfterEach
    void tearDown() {
        clearTables();
    }

    private void clearTables() {
        jdbcTemplate.execute("DELETE FROM saved_spot_archives");
        jdbcTemplate.execute("DELETE FROM spots");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    @DisplayName("가입 지표: 대상일 신규 가입은 탈퇴 유저를 포함하고 provider별로 분리되며, 누적은 이전 가입을 포함한다")
    void aggregatesSignupStatistics() {
        insertUser("K", "kakao-yesterday", TARGET.atTime(12, 0), null);       // 어제 가입, 활성
        insertUser("A", "apple-yesterday", TARGET.atTime(12, 0), LocalDateTime.now()); // 어제 가입, 탈퇴
        insertUser("K", "kakao-2days", TARGET.minusDays(1).atTime(12, 0), null);       // 이틀 전 가입

        StatisticsSnapshot snapshot = metricsAggregationService.aggregate(TARGET);

        assertThat(snapshot.date()).isEqualTo(TARGET);
        assertThat(snapshot.newSignups()).isEqualTo(2);        // 탈퇴 포함
        assertThat(snapshot.newSignupsKakao()).isEqualTo(1);
        assertThat(snapshot.newSignupsApple()).isEqualTo(1);
        assertThat(snapshot.cumulativeSignups()).isEqualTo(3); // 이틀 전 가입까지 누적
    }

    @Test
    @DisplayName("저장 지표: 활성 저장 유저 수/비율은 활성 유저 기준이고, TOP 스팟은 활성 저장 건수 순으로 정렬된다")
    void aggregatesSaveStatistics() {
        Long userA = insertUser("K", "saver-a", TARGET.atTime(9, 0), null);            // 활성
        Long userB = insertUser("A", "saver-b", TARGET.atTime(9, 0), LocalDateTime.now()); // 탈퇴
        Long userC = insertUser("K", "saver-c", TARGET.atTime(9, 0), null);            // 활성

        Long spot1 = spotRepository.save(buildSpot("한강")).getId();
        Long spot2 = spotRepository.save(buildSpot("남산")).getId();

        insertActiveSave(userA, spot1);
        insertActiveSave(userA, spot2);
        insertActiveSave(userB, spot1); // 탈퇴 유저의 활성 저장
        insertActiveSave(userC, spot1);

        StatisticsSnapshot snapshot = metricsAggregationService.aggregate(TARGET);

        assertThat(snapshot.totalUsers()).isEqualTo(2);                 // 활성 유저 A, C
        assertThat(snapshot.activeSavers()).isEqualTo(2);               // A, C (B는 탈퇴로 제외)
        assertThat(snapshot.saveUsageRatio()).isEqualTo(1.0);           // 2 / 2
        assertThat(snapshot.topSpots()).isEqualTo("한강(3), 남산(1)");   // spot1=3, spot2=1
    }

    @Test
    @DisplayName("소프트 삭제된 저장은 집계에서 제외된다")
    void excludesSoftDeletedSaves() {
        Long userA = insertUser("K", "soft-a", TARGET.atTime(9, 0), null);
        Long spot1 = spotRepository.save(buildSpot("한강")).getId();
        insertSoftDeletedSave(userA, spot1);

        StatisticsSnapshot snapshot = metricsAggregationService.aggregate(TARGET);

        assertThat(snapshot.activeSavers()).isZero();
        assertThat(snapshot.topSpots()).isEmpty();
    }

    @Test
    @DisplayName("활성 유저가 없으면 저장 비율은 0으로 처리된다 (0 나눗셈 방어)")
    void ratioIsZeroWhenNoActiveUsers() {
        StatisticsSnapshot snapshot = metricsAggregationService.aggregate(TARGET);

        assertThat(snapshot.totalUsers()).isZero();
        assertThat(snapshot.saveUsageRatio()).isZero();
    }

    private Long insertUser(String providerCode, String providerUserId, LocalDateTime createdAt,
                            LocalDateTime deletedAt) {
        jdbcTemplate.update(
            "INSERT INTO users (created_at, updated_at, provider, provider_user_id, role, archive_name, deleted_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)",
            Timestamp.valueOf(createdAt), Timestamp.valueOf(createdAt), providerCode, providerUserId,
            "C", "나의 보관함", deletedAt == null ? null : Timestamp.valueOf(deletedAt));
        return jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE provider_user_id = ?", Long.class, providerUserId);
    }

    private void insertActiveSave(Long userId, Long spotId) {
        insertSave(userId, spotId, null);
    }

    private void insertSoftDeletedSave(Long userId, Long spotId) {
        insertSave(userId, spotId, LocalDateTime.now());
    }

    private void insertSave(Long userId, Long spotId, LocalDateTime deletedAt) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
            "INSERT INTO saved_spot_archives (created_at, updated_at, user_id, spot_id, deleted_at) "
                + "VALUES (?, ?, ?, ?, ?)",
            Timestamp.valueOf(now), Timestamp.valueOf(now), userId, spotId,
            deletedAt == null ? null : Timestamp.valueOf(deletedAt));
    }

    private Spot buildSpot(String name) {
        return Spot.builder()
            .name(name)
            .theme(SpotTheme.SUNSET)
            .latitude(37.55)
            .longitude(126.99)
            .status(SpotStatus.PUBLISHED)
            .build();
    }
}
