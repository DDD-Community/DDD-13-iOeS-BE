package com.ioes.photo.external.weather.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 단기예보 강수형태 코드.
 *
 * <p>기상청 단기예보 API의 PTY 카테고리 값에 대응합니다.</p>
 *
 * @author 김성민
 */
@Getter
@RequiredArgsConstructor
public enum PrecipitationType {

    NONE("0", "없음"),
    RAIN("1", "비"),
    RAIN_SNOW("2", "비/눈"),
    SNOW("3", "눈"),
    SHOWER("4", "소나기");

    private final String code;
    private final String description;

    private static final Map<String, PrecipitationType> CODE_MAP =
        Stream.of(values()).collect(Collectors.toMap(PrecipitationType::getCode, Function.identity()));

    public static PrecipitationType fromCode(String code) {
        PrecipitationType type = CODE_MAP.get(code);
        if (type == null) {
            throw new IllegalArgumentException("알 수 없는 강수유무 코드: " + code);
        }
        return type;
    }
}
