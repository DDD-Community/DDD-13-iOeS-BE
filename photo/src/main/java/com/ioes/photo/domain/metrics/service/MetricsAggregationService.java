package com.ioes.photo.domain.metrics.service;

import com.ioes.photo.domain.metrics.dto.MetricsSnapshot;
import com.ioes.photo.domain.metrics.mapper.MetricsMapper;
import com.ioes.photo.domain.metrics.mapper.ProviderCountRow;
import com.ioes.photo.global.auth.oauth.OAuthProvider;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영 지표 집계 서비스.
 *
 * 대상 일자를 받아 가입/저장 지표를 집계한 {@link MetricsSnapshot}을 만든다.
 * 노션 연동과 무관한 순수 집계 계층으로, 스케줄러가 "어제" 일자를 넘겨 호출한다.
 *
 * @author 김성민
 */
@Service
@RequiredArgsConstructor
public class MetricsAggregationService {

    private static final int TOP_SPOT_LIMIT = 5;

    private final MetricsMapper metricsMapper;

    @Transactional(readOnly = true)
    public MetricsSnapshot aggregate(LocalDate targetDate) {
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        long newSignups = metricsMapper.countSignups(start, end);
        Map<String, Long> byProvider = metricsMapper.countSignupsByProvider(start, end).stream()
            .collect(Collectors.toMap(ProviderCountRow::provider, ProviderCountRow::signupCount));
        long kakao = byProvider.getOrDefault(OAuthProvider.KAKAO.getCode(), 0L);
        long apple = byProvider.getOrDefault(OAuthProvider.APPLE.getCode(), 0L);

        long cumulative = metricsMapper.countCumulativeSignups(end);
        long activeSavers = metricsMapper.countActiveSavers();
        long totalUsers = metricsMapper.countActiveUsers();
        double saveUsageRatio = totalUsers == 0 ? 0.0 : (double) activeSavers / totalUsers;

        String topSpots = metricsMapper.findTopSavedSpots(TOP_SPOT_LIMIT).stream()
            .map(row -> row.spotName() + "(" + row.saveCount() + ")")
            .collect(Collectors.joining(", "));

        return new MetricsSnapshot(
            targetDate, newSignups, kakao, apple,
            cumulative, activeSavers, totalUsers, saveUsageRatio, topSpots
        );
    }
}
