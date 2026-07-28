package com.ioes.photo.global.common.validation;

import java.math.BigDecimal;

/**
 * 좌표(위도/경도) 검증 공통 로직.
 *
 * <p>기존에는 {@code @DecimalMin}/{@code @DecimalMax}/{@code @Digits} 합성 제약으로 검증했으나,
 * {@code @Digits}가 내부에서 {@code new BigDecimal(value.toString())}을 호출하므로
 * {@code NaN}/{@code Infinity} 입력 시 {@code NumberFormatException}이 던져지고,
 * 이것이 {@code jakarta.validation.ValidationException}(HV000028)으로 전파되어 500으로 이어졌다.
 *
 * <p>이 검증기는 예외를 던지지 않고 유한성·범위·소수 자릿수를 순서대로 확인하여
 * 비유한값을 정상적인 제약 위반(400)으로 처리한다.
 *
 * @author 김성민
 */
final class CoordinateValidatorSupport {

    private CoordinateValidatorSupport() {
    }

    /**
     * 좌표 유효성 검증.
     *
     * @param value       검증 대상 값 (null은 통과 — 필수 여부는 {@code @NotNull}이 담당)
     * @param min         허용 최솟값(포함)
     * @param max         허용 최댓값(포함)
     * @param maxFraction 허용 소수 자릿수
     * @return 유효하면 true
     */
    static boolean isValid(Double value, double min, double max, int maxFraction) {
        if (value == null) {
            return true;
        }
        if (!Double.isFinite(value)) {
            return false;
        }
        if (value < min || value > max) {
            return false;
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().scale() <= maxFraction;
    }
}
