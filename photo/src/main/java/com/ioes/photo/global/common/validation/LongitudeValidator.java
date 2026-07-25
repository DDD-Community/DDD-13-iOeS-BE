package com.ioes.photo.global.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link Longitude} 제약 검증기.
 *
 * <p>범위(-180.0 ~ 180.0)와 소수 6자리를 검증하며, {@code NaN}/{@code Infinity}를
 * 예외 없이 제약 위반으로 처리한다.
 *
 * @author 김성민
 */
public class LongitudeValidator implements ConstraintValidator<Longitude, Double> {

    @Override
    public boolean isValid(Double value, ConstraintValidatorContext context) {
        return CoordinateValidatorSupport.isValid(value, -180.0, 180.0, 6);
    }
}
