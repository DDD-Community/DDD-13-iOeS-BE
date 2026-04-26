package com.ioes.photo.domain.spot.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 스팟 테마.
 *
 * @author 김성민
 */
@Getter
@RequiredArgsConstructor
public enum SpotTheme implements CodedEnum {
    SUNSET("SS"),
    YUNSEUL("YS");

    private final String code;
}
