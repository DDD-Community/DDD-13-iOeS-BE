package com.ioes.photo.external.crowd.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 서울시 실시간 인구 혼잡도 단계.
 *
 * <p>면적 대비 인구 밀집도를 기반으로 4단계로 산출됩니다.</p>
 *
 * @author 김성민
 */
@Getter
@RequiredArgsConstructor
public enum CongestionLevel implements CodedEnum {

    RELAXED("R", "여유", "인구가 평소와 비교하여 적음"),
    NORMAL("N", "보통", "인구가 평소와 비교하여 비슷함"),
    SLIGHTLY_CROWDED("S", "약간 붐빔", "인구가 평소와 비교하여 많음"),
    CROWDED("C", "붐빔", "인구가 평소와 비교하여 매우 많음");

    private final String code;
    private final String label;
    private final String description;

    private static final Map<String, CongestionLevel> LABEL_MAP =
        Stream.of(values()).collect(Collectors.toMap(CongestionLevel::getLabel, Function.identity()));

    public static CongestionLevel fromLabel(String label) {
        CongestionLevel level = LABEL_MAP.get(label);
        if (level == null) {
            throw new IllegalArgumentException("알 수 없는 혼잡도: " + label);
        }
        return level;
    }

    /**
     * 관광지 집중률 예측 지수(0~100)를 혼잡도 단계로 변환한다.
     *
     * <p>구간 기준은 대전 관광지 115곳 × 30일 집중률 분포의 사분위를 기반으로
     * 기획과 합의한 값이다: 여유 &lt;30 / 보통 30~45 / 약간 붐빔 45~70 / 붐빔 ≥70.</p>
     */
    public static CongestionLevel fromRate(double cnctrRate) {
        if (cnctrRate >= 70) {
            return CROWDED;
        }
        if (cnctrRate >= 45) {
            return SLIGHTLY_CROWDED;
        }
        if (cnctrRate >= 30) {
            return NORMAL;
        }
        return RELAXED;
    }
}
