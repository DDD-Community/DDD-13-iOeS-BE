package com.ioes.photo.domain.notification.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnum;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 검수완료 알림 히스토리 확인 여부 플래그.
 *
 * 히스토리 생성 시 기본값은 N 이며, 사용자가 확인 처리 API를 호출하면 Y 로 전환된다.
 *
 * @author 황제연
 */
@Getter
@RequiredArgsConstructor
public enum CheckYn implements CodedEnum {
    Y("Y"),
    N("N");

    private static final Map<String, CheckYn> CODE_INDEX =
        Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(CheckYn::getCode, Function.identity()));

    private final String code;

    public static CheckYn fromCode(String code) {
        CheckYn value = CODE_INDEX.get(code);
        if (value == null) {
            throw new IllegalArgumentException("알 수 없는 CheckYn code: " + code);
        }
        return value;
    }
}
