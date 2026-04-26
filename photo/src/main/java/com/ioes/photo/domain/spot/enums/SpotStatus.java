package com.ioes.photo.domain.spot.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 스팟 공개 상태.
 *
 * 사용자 등록 스팟은 기본 PENDING 으로 생성되며, 운영자 검수 이후 PUBLISHED/REJECTED 로 전이된다.
 *
 * @author 김성민
 */
@Getter
@RequiredArgsConstructor
public enum SpotStatus implements CodedEnum {
    PENDING("P"),
    PUBLISHED("B"),
    REJECTED("R");

    private final String code;
}
