package com.ioes.photo.domain.spot.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 스팟 테마.
 *
 * @author 김성민, 황제연
 */
@Getter
@RequiredArgsConstructor
public enum SpotTheme implements CodedEnum {
    SUNSET("SS"),
    YUNSEUL("YS");

    private final String code;

    public static SpotTheme fromCode(String code) {
        for (SpotTheme theme : values()) {
            if (theme.getCode().equals(code)) {
                return theme;
            }
        }
        throw new IllegalArgumentException("알 수 없는 SpotTheme 코드: " + code);
    }
}
