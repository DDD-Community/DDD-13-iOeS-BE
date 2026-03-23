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

    /*
        요청 성공 여부
     */
    private final boolean success;
    /*
        응답코드
     */
    private final String code;
    /*
        응답 메세지
     */
    private final String message;
    /*
        요청 성공할 경우에만 포함하는 데이터
     */
    private final T data;

    /*
        데이터가 있는 성공 응답
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "S000", "성공", data);
    }

    /*
        데이터가 없는 성공 응답
     */
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, "S000", "성공", null);
    }

    /**
     * {@link ErrorCode}의 기본 메시지를 사용한 에러 응답을 생성합니다.
     *
     * @param errorCode 에러 코드
     * @return 에러 응답
     */
    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 커스텀 메시지를 사용한 에러 응답을 생성합니다.
     *
     * @param errorCode 에러 코드
     * @param message   커스텀 에러 메시지
     * @return 에러 응답
     */
    public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, errorCode.getCode(), message, null);
    }
}