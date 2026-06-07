package com.ioes.photo.domain.spot.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnum;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    private static final Map<String, SpotStatus> CODE_INDEX =
        Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(SpotStatus::getCode, Function.identity()));

    private final String code;

    public static SpotStatus fromCode(String code) {
        SpotStatus status = CODE_INDEX.get(code);
        if (status == null) {
            throw new IllegalArgumentException("알 수 없는 SpotStatus code: " + code);
        }
        return status;
    }
}
