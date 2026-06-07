package com.ioes.photo.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ioes.photo.global.error.code.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 모든 API 엔드포인트의 공통 응답 래퍼 클래스
 * 성공 응답과 에러 응답을 제공합니다
 *
 *
 * @param <T> 응답 데이터 타입
 * @author 황제연
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {


    private final boolean success;
    private final String code;
    private final String message;
    private final T data;
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "S000", "성공", data);
    }
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, "S000", "성공", null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, errorCode.getCode(), message, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message, T data) {
        return new ApiResponse<>(false, errorCode.getCode(), message, data);
    }
}