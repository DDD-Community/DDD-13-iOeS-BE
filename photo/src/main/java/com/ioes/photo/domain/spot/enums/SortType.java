package com.ioes.photo.domain.spot.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 스팟 목록 정렬 방식.
 *
 * @author 황제연
 */
@Getter
@RequiredArgsConstructor
public enum SortType implements CodedEnum {
    DISTANCE("D"),
    RECOMMENDED("R");

    private final String code;
}
