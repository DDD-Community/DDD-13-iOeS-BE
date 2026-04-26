package com.ioes.photo.global.persistence.enumeration;

import jakarta.persistence.AttributeConverter;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link CodedEnum} 구현 enum을 DB 코드 문자열로 변환하는 추상 컨버터.
 *
 * <p>각 enum 별로 이 클래스를 상속하고 {@code @Converter(autoApply = true)} 를 부여하면,
 * 별도 매핑 없이 모든 엔티티의 해당 enum 필드가 자동으로 코드 문자열로 저장된다.</p>
 *
 * @param <E> 대상 enum 타입
 * @author 김성민
 */
public abstract class CodedEnumConverter<E extends Enum<E> & CodedEnum>
    implements AttributeConverter<E, String> {

    private final Class<E> enumClass;
    private final Map<String, E> codeIndex;

    protected CodedEnumConverter(Class<E> enumClass) {
        this.enumClass = enumClass;
        this.codeIndex = EnumSet.allOf(enumClass).stream()
            .collect(Collectors.toUnmodifiableMap(CodedEnum::getCode, Function.identity()));
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        E value = codeIndex.get(dbData);
        if (value == null) {
            throw new IllegalArgumentException(
                "알 수 없는 코드: enum=" + enumClass.getSimpleName() + " code=" + dbData);
        }
        return value;
    }
}
