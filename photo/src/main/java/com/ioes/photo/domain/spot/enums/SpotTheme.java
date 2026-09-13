package com.ioes.photo.domain.spot.enums;

import com.ioes.photo.global.common.util.NullUtils;
import com.ioes.photo.global.persistence.enumeration.CodedEnum;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 스팟 테마.
 *
 * 코드는 영문명에서 고안하되 기존 코드와 중복되지 않아야 한다.
 * DB에 이미 적재된 값이므로 한 번 부여한 코드는 변경하지 않는다.
 *
 * @author 김성민, 황제연
 */
@Getter
@RequiredArgsConstructor
public enum SpotTheme implements CodedEnum {
    SUNSET("SS", "노을"),
    YUNSEUL("YS", "윤슬"),
    SUNLIGHT("SL", "햇살"),
    NIGHT_VIEW("NV", "야경");

    private static final Map<String, SpotTheme> CODE_INDEX =
        Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(SpotTheme::getCode, Function.identity()));

    private static final Map<String, SpotTheme> LABEL_INDEX =
        Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(SpotTheme::getLabel, Function.identity()));

    private final String code;
    private final String label;

    public static SpotTheme fromCode(String code) {
        SpotTheme theme = NullUtils.isBlank(code) ? null : CODE_INDEX.get(code);
        if (theme == null) {
            throw new IllegalArgumentException("알 수 없는 SpotTheme 코드: " + code);
        }
        return theme;
    }

    public static SpotTheme fromLabel(String label) {
        SpotTheme theme = NullUtils.isBlank(label) ? null : LABEL_INDEX.get(label);
        if (theme == null) {
            throw new IllegalArgumentException("알 수 없는 SpotTheme 이름: " + label);
        }
        return theme;
    }

    public static String labelGuide() {
        return Arrays.stream(values())
            .map(theme -> theme.name() + "=" + theme.label)
            .collect(Collectors.joining(", "));
    }
}
