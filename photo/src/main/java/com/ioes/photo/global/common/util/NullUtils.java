package com.ioes.photo.global.common.util;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Null 및 빈 값 처리 편의성 유틸리티 클래스
 *
 * @author 황제연
 */
public final class NullUtils {

    private NullUtils() {}

    public static <T> T requireNonNull(T value) {
        return Objects.requireNonNull(value);
    }

    public static <T> T requireNonNull(T value, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value == null) {
            throw exceptionSupplier.get();
        }
        return value;
    }

    public static <T> T orElseThrow(Optional<T> optional) {
        return optional.orElseThrow();
    }

    public static <T> T orElseThrow(Optional<T> optional, Supplier<? extends RuntimeException> exceptionSupplier) {
        return optional.orElseThrow(exceptionSupplier);
    }

    public static <T> T orDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static <T> T orDefault(T value, Supplier<T> defaultSupplier) {
        return value != null ? value : defaultSupplier.get();
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    public static String orDefaultIfBlank(String value, String defaultValue) {
        return isNotBlank(value) ? value : defaultValue;
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }
}