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

    /**
     * null이 아닌 값이면 그대로 반환합니다.
     * null이면 NullPointerException을 발생시킵니다.
     */
    public static <T> T requireNonNull(T value) {
        return Objects.requireNonNull(value);
    }

    /**
     * null이면 xceptionSupplier가 제공하는 예외를 발생시킵니다.
     *
     */
    public static <T> T requireNonNull(T value, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value == null) {
            throw exceptionSupplier.get();
        }
        return value;
    }

    /**
     * Optional이 비어있으면 NoSuchElementException을 발생시킵니다.
     */
    public static <T> T orElseThrow(Optional<T> optional) {
        return optional.orElseThrow();
    }

    /**
     * Optional이 비어있으면 exceptionSupplier가 제공하는 예외를 발생시킵니다.
     *
     */
    public static <T> T orElseThrow(Optional<T> optional, Supplier<? extends RuntimeException> exceptionSupplier) {
        return optional.orElseThrow(exceptionSupplier);
    }


    /**
     * null이면 defaultValue 반환
     * @param value
     * @param defaultValue
     * @return
     * @param <T>
     */
    public static <T> T orDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * null이면 defaultSupplier로부터 기본값 반환
     * @param value
     * @param defaultSupplier
     * @return
     * @param <T>
     */
    public static <T> T orDefault(T value, Supplier<T> defaultSupplier) {
        return value != null ? value : defaultSupplier.get();
    }


    /**
     * null이거나 blank이면 true
     * @param value
     * @return
     */
    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * null이 아니고 blank도 아니면 true
     * @param value
     * @return
     */
    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    /**
     * blank이면 defaultValue
     * @param value
     * @param defaultValue
     * @return
     */
    public static String orDefaultIfBlank(String value, String defaultValue) {
        return isNotBlank(value) ? value : defaultValue;
    }

    /**
     * null이거나 비어있으면 true
     * @param collection
     * @return
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * null이 아니고 비어있지 않으면 true
     * @param collection
     * @return
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }
}