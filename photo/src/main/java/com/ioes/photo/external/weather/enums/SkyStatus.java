package com.ioes.photo.external.weather.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 단기예보 하늘상태 코드.
 *
 * <p>기상청 단기예보 API의 SKY 카테고리 값에 대응합니다.</p>
 *
 * @author 김성민
 */
@Getter
@RequiredArgsConstructor
public enum SkyStatus {

    CLEAR("1", "맑음"),
    MOSTLY_CLOUDY("3", "구름많음"),
    OVERCAST("4", "흐림");

    private final String code;
    private final String description;

    private static final Map<String, SkyStatus> CODE_MAP =
        Stream.of(values()).collect(Collectors.toMap(SkyStatus::getCode, Function.identity()));

    public static SkyStatus fromCode(String code) {
        SkyStatus status = CODE_MAP.get(code);
        if (status == null) {
            throw new IllegalArgumentException("알 수 없는 하늘상태 코드: " + code);
        }
        return status;
    }
}
