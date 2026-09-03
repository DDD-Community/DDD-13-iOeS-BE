package com.ioes.photo.domain.spot.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnum;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 스팟 노출(release) on/off 플래그.
 *
 * 검수 상태(status)와 독립적으로 지도뷰/리스트 조회 노출 여부만 제어한다.
 * PUBLISHED 승인 시 자동으로 Y 가 되며, 이후 소유자가 별도 API로 껐다 켤 수 있다.
 *
 * @author 황제연
 */
@Getter
@RequiredArgsConstructor
public enum RelYn implements CodedEnum {
    Y("Y"),
    N("N");

    private static final Map<String, RelYn> CODE_INDEX =
        Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(RelYn::getCode, Function.identity()));

    private final String code;

    public static RelYn fromCode(String code) {
        RelYn value = CODE_INDEX.get(code);
        if (value == null) {
            throw new IllegalArgumentException("알 수 없는 RelYn code: " + code);
        }
        return value;
    }
}
