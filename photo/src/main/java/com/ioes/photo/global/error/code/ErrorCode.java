package com.ioes.photo.global.error.code;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드 계약을 정의하는 인터페이스.
 *
 * 모든 에러 코드는 이 인터페이스를 구현하며, BusinessException과 ApiResponse에서 에러 정보를 표준화하는 데 사용됩니다.
 *
 *
 * @see com.ioes.photo.global.error.code.CommonErrorCode
 * @see com.ioes.photo.global.error.exception.BusinessException
 * @author 황제연
 */
public interface ErrorCode {
    String getCode();
    String getMessage();
    HttpStatus getStatus();
}