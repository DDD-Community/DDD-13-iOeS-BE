package com.ioes.photo.global.config.web.converter;

import com.ioes.photo.global.persistence.enumeration.CodedEnum;
import java.util.Optional;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.format.FormatterRegistry;

/**
 * {@link CodedEnum} 을 구현한 모든 enum을 스캔해 {@link StringToCodedEnumConverter} 로 등록하는 레지스트라.
 *
 * <p>스캔 대상마다 String → 구체 enum class 의 정확한 타입쌍으로 등록하므로,
 * Spring 기본 제공 컨버터인 {@code StringToEnumConverterFactory}(String → Enum, 상위타입 키)보다
 * {@link org.springframework.core.convert.support.GenericConversionService} 내부 탐색 순서상 항상 먼저 매치된다.</p>
 *
 * @author 황제연
 */
public class CodedEnumConverterRegistrar {

    public void registerAll(FormatterRegistry registry, String basePackage) {
        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(CodedEnum.class));

        for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
            resolveEnumClass(candidate.getBeanClassName())
                .ifPresent(enumClass -> register(registry, enumClass));
        }
    }

    private Optional<Class<?>> resolveEnumClass(String className) {
        try {
            Class<?> candidateClass = Class.forName(className);
            if (candidateClass.isEnum() && CodedEnum.class.isAssignableFrom(candidateClass)) {
                return Optional.of(candidateClass);
            }
            return Optional.empty();
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void register(FormatterRegistry registry, Class<?> enumClass) {
        registerTyped(registry, (Class) enumClass);
    }

    private <E extends Enum<E> & CodedEnum> void registerTyped(FormatterRegistry registry, Class<E> enumClass) {
        registry.addConverter(String.class, enumClass, new StringToCodedEnumConverter<>(enumClass));
    }
}
