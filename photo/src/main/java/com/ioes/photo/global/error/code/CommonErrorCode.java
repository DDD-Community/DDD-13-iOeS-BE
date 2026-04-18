package com.ioes.photo.global.error.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전역 공통 에러 코드 정의.
 *
 * 도메인에 관계없이 공통으로 발생할 수 있는 에러 상황을 정의합니다.
 * 도메인 별 에러 코드는 각 도메인 패키지에서 별도 구현체로 정의하세요.
 *
 *
 *
 * - C001: 입력값 오류 (400)</li>
 * - C002: 타입 오류 (400)</li>
 * - C003: 필수 파라미터 누락 (400)</li>
 * - C004: 인증 필요 (401)</li>
 * - C005: 접근 권한 없음 (403)</li>
 * - C006: 리소스 없음 (404)</li>
 * - C007: HTTP 메서드 불가 (405)</li>
 * - C008: 리소스 충돌 (409)</li>
 * - C999: 서버 내부 오류 (500)</li>
 *
 * 코드는 이후 논의 후 변경 예정
 *
 * @author 황제연
 */
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    INVALID_INPUT_VALUE("C001", "입력값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_TYPE_VALUE("C002", "요청 타입이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    MISSING_REQUEST_PARAMETER("C003", "필수 요청 파라미터가 누락되었습니다.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("C004", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("C005", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND("C006", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("C007", "지원하지 않는 HTTP 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED),
    CONFLICT("C008", "이미 존재하는 리소스입니다.", HttpStatus.CONFLICT),
    INTERNAL_SERVER_ERROR("C999", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;
}