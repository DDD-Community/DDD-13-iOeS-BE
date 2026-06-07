package com.ioes.photo.global.error.exception;

import com.ioes.photo.global.error.code.ErrorCode;
import lombok.Getter;

/**
 * 비즈니스 로직에서 발생하는 예외의 기본 클래스.
 *
 * ErrorCode를 포함하여 에러 코드, 메시지, HTTP 상태를 구조화합니다.
 *
 * @author 황제연
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}