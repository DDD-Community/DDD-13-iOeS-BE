package com.ioes.photo.domain.statistics.service;

import com.ioes.photo.domain.statistics.dto.StatisticsSnapshot;
import com.ioes.photo.domain.statistics.mapper.StatisticsMapper;
import com.ioes.photo.domain.statistics.mapper.ProviderCountRow;
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
 * 대상 일자를 받아 가입/저장 지표를 집계한 {@link StatisticsSnapshot}을 만든다.
 * 노션 연동과 무관한 순수 집계 계층으로, 스케줄러가 "어제" 일자를 넘겨 호출한다.
 *
 * @author 김성민
 */
@Service
@RequiredArgsConstructor
public class StatisticsAggregationService {

    private static final int TOP_SPOT_LIMIT = 5;

    private final StatisticsMapper statisticsMapper;

    @Transactional(readOnly = true)
    public StatisticsSnapshot aggregate(LocalDate targetDate) {
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        long newSignups = statisticsMapper.countSignups(start, end);
        Map<String, Long> byProvider = statisticsMapper.countSignupsByProvider(start, end).stream()
            .collect(Collectors.toMap(ProviderCountRow::provider, ProviderCountRow::signupCount));
        long kakao = byProvider.getOrDefault(OAuthProvider.KAKAO.getCode(), 0L);
        long apple = byProvider.getOrDefault(OAuthProvider.APPLE.getCode(), 0L);

        long cumulative = statisticsMapper.countCumulativeSignups(end);
        long activeSavers = statisticsMapper.countActiveSavers();
        long totalUsers = statisticsMapper.countActiveUsers();
        double saveUsageRatio = roundRatio(totalUsers == 0 ? 0.0 : (double) activeSavers / totalUsers);

        String topSpots = statisticsMapper.findTopSavedSpots(TOP_SPOT_LIMIT).stream()
            .map(row -> row.spotName() + "(" + row.saveCount() + ")")
            .collect(Collectors.joining(", "));

        return new StatisticsSnapshot(
            targetDate, newSignups, kakao, apple,
            cumulative, activeSavers, totalUsers, saveUsageRatio, topSpots
        );
    }

    /** 저장 사용 비율을 소수점 4자리로 반올림한다 (퍼센트 표기 시 소수 2자리). */
    private double roundRatio(double ratio) {
        return Math.round(ratio * 10000) / 10000.0;
    }
}
