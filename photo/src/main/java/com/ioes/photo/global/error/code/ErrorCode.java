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

    /**
     * 에러 코드 문자열을 반환합니다 (예: "C001", "O002").
     *
     * @return 에러 코드 문자열
     */
    String getCode();

    /**
     * 사용자에게 노출할 에러 메시지를 반환합니다.
     *
     * @return 에러 메시지
     */
    String getMessage();

    /**
     * HTTP 응답 상태 코드를 반환합니다.
     *
     * @return HTTP 상태 코드
     */
    HttpStatus getStatus();
}