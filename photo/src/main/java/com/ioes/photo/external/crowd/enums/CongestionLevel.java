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
}
