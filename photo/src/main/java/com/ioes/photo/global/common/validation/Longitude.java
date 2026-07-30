package com.ioes.photo.global.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 경도(longitude) 유효성 검증 어노테이션.
 *
 * - 범위: -180.0 ~ 180.0
 * - 소수점 최대 6자리
 * - NaN/Infinity는 제약 위반으로 처리 (예외를 던지지 않음)
 *
 * @author 황제연
 */
@Documented
@Constraint(validatedBy = LongitudeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Longitude {

    String message() default "유효하지 않은 경도 값입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
