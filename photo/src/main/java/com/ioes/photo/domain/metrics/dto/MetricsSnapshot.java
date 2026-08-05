package com.ioes.photo.domain.metrics.dto;

import java.time.LocalDate;

/**
 * 특정 일자 기준 운영 지표 스냅샷.
 *
 * 가입 지표는 {@code date} 하루치(신규)와 {@code date} 종료 시점까지의 누적을 담고,
 * 저장 지표는 집계 실행 시점의 현재 값을 담는다.
 *
 * @param date              대상 일자 (KST)
 * @param newSignups        대상일 신규 가입자 수 (탈퇴 포함)
 * @param newSignupsKakao   대상일 카카오 신규 가입자 수
 * @param newSignupsApple   대상일 애플 신규 가입자 수
 * @param cumulativeSignups 대상일 종료 시점까지 누적 가입자 수 (탈퇴 포함)
 * @param activeSavers      활성 저장을 보유한 활성 유저 수
 * @param totalUsers        활성(미탈퇴) 유저 수
 * @param saveUsageRatio    저장 기능 사용 유저 비율 (0.0 ~ 1.0, activeSavers / totalUsers)
 * @param topSpots          저장 수 상위 스팟 요약 (예: "여의도한강공원(31), 남산타워(22)")
 * @author 김성민
 */
public record MetricsSnapshot(
    LocalDate date,
    long newSignups,
    long newSignupsKakao,
    long newSignupsApple,
    long cumulativeSignups,
    long activeSavers,
    long totalUsers,
    double saveUsageRatio,
    String topSpots
) {
}
