package com.ioes.photo.domain.spot.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnum;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 스팟 출처 구분.
 *
 * 운영자가 등록한 큐레이션 스팟과 사용자가 등록한 UGC 스팟을 구분한다.
 * V2 UGC 기능 대비 선반영 필드로, 현재는 값 적재까지만 사용한다.
 *
 * @author 김성민
 */
@Getter
@RequiredArgsConstructor
public enum SpotSource implements CodedEnum {
    CURATION("C"),
    UGC("U");

    private static final Map<String, SpotSource> CODE_INDEX =
        Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(SpotSource::getCode, Function.identity()));

    private final String code;

    public static SpotSource fromCode(String code) {
        SpotSource source = CODE_INDEX.get(code);
        if (source == null) {
            throw new IllegalArgumentException("알 수 없는 SpotSource code: " + code);
        }
        return source;
    }
}
