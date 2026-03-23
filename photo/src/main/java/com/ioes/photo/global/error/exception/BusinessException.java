package com.ioes.photo.global.error.exception;

import com.ioes.photo.global.error.code.ErrorCode;
import lombok.Getter;

/**
 * 비즈니스 로직에서 발생하는 예외의 기본 클래스.
 *
 * {@link ErrorCode}를 포함하여 에러 코드, 메시지, HTTP 상태를 구조화합니다.
 *
 * @author 황제연
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 이 예외와 연관된 에러 코드 */
    private final ErrorCode errorCode;

    /**
     * {@link ErrorCode}의 기본 메시지로 예외를 생성합니다.
     *
     * @param errorCode 에러 코드
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 커스텀 메시지로 예외를 생성합니다.
     *
     * @param errorCode 에러 코드 (HTTP 상태, 코드 결정에 사용)
     * @param message   사용자에게 노출할 커스텀 메시지
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}