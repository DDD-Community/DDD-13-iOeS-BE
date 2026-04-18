package com.ioes.photo.external.error;

import com.ioes.photo.global.error.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 외부 API 연동 전용 에러 코드 정의.
 *
 * <ul>
 *   <li>E001: 외부 API 호출 실패 (502)</li>
 *   <li>E002: 응답 파싱 실패 (502)</li>
 *   <li>E003: 응답 시간 초과 (504)</li>
 *   <li>E004: 인증키 오류 (401)</li>
 *   <li>E005: 일일 호출 한도 초과 (429)</li>
 * </ul>
 *
 * @author 김성민
 */
@Getter
@RequiredArgsConstructor
public enum ExternalApiErrorCode implements ErrorCode {

    API_CALL_FAILED("E001", "외부 API 호출에 실패했습니다.", HttpStatus.BAD_GATEWAY),
    API_RESPONSE_PARSE_FAILED("E002", "외부 API 응답 파싱에 실패했습니다.", HttpStatus.BAD_GATEWAY),
    API_TIMEOUT("E003", "외부 API 응답 시간이 초과되었습니다.", HttpStatus.GATEWAY_TIMEOUT),
    API_SERVICE_KEY_INVALID("E004", "외부 API 인증키가 유효하지 않습니다.", HttpStatus.UNAUTHORIZED),
    API_RATE_LIMIT_EXCEEDED("E005", "외부 API 일일 호출 한도를 초과했습니다.", HttpStatus.TOO_MANY_REQUESTS);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
