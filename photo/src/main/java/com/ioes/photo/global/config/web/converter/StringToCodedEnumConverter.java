package com.ioes.photo.global.config.web.converter;

import com.ioes.photo.global.persistence.enumeration.CodedEnum;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;

/**
 * 문자열을 {@link CodedEnum} 구현 enum으로 변환하는 컨버터.
 *
 * <p>DB 코드 값을 우선 매칭하고, 일치하는 code가 없으면 enum 이름({@link Enum#valueOf})으로 폴백한다.
 * 예를 들어 {@code SpotTheme}는 "SS"(code)와 "SUNSET"(name) 둘 다로 바인딩된다.</p>
 *
 * <p>생성 시점에 어떤 상수의 이름이 다른 상수의 code와 겹치는 모호성이 있는지 검증해 즉시 실패한다.
 * 이런 모호성이 있으면 code 우선 매칭 규칙상 name으로는 절대 도달할 수 없는 상수가 생기기 때문이다.</p>
 *
 * @param <E> 대상 enum 타입
 * @author 황제연
 */
public class StringToCodedEnumConverter<E extends Enum<E> & CodedEnum> implements Converter<String, E> {

    private final Class<E> enumClass;
    private final Map<String, E> codeIndex;

    public StringToCodedEnumConverter(Class<E> enumClass) {
        this.enumClass = enumClass;
        this.codeIndex = EnumSet.allOf(enumClass).stream()
            .collect(Collectors.toUnmodifiableMap(CodedEnum::getCode, Function.identity()));
        validateNoAmbiguity();
    }

    @Override
    public E convert(String source) {
        String trimmed = source.trim();
        E byCode = codeIndex.get(trimmed);
        if (byCode != null) {
            return byCode;
        }
        return Enum.valueOf(enumClass, trimmed);
    }

    private void validateNoAmbiguity() {
        for (E constant : EnumSet.allOf(enumClass)) {
            E matchedByCode = codeIndex.get(constant.name());
            if (matchedByCode != null && matchedByCode != constant) {
                throw new IllegalStateException(
                    "CodedEnum 모호성: " + enumClass.getSimpleName() + "." + constant.name()
                        + " 이(가) " + matchedByCode.name() + "의 code(\"" + matchedByCode.getCode()
                        + "\")와 겹쳐 name으로는 도달할 수 없습니다.");
            }
        }
    }
}
