package com.ioes.photo.global.error.code;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CommonErrorCode} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("CommonErrorCode 테스트")
class CommonErrorCodeTest {

    @Test
    @DisplayName("INVALID_INPUT_VALUE - 코드/메시지/상태 확인")
    void invalidInputValue() {
        CommonErrorCode code = CommonErrorCode.INVALID_INPUT_VALUE;
        assertThat(code.getCode()).isEqualTo("C001");
        assertThat(code.getMessage()).isNotBlank();
        assertThat(code.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("INVALID_TYPE_VALUE - 상태 확인")
    void invalidTypeValue() {
        assertThat(CommonErrorCode.INVALID_TYPE_VALUE.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(CommonErrorCode.INVALID_TYPE_VALUE.getCode()).isEqualTo("C002");
    }

    @Test
    @DisplayName("MISSING_REQUEST_PARAMETER - 상태 확인")
    void missingRequestParameter() {
        assertThat(CommonErrorCode.MISSING_REQUEST_PARAMETER.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(CommonErrorCode.MISSING_REQUEST_PARAMETER.getCode()).isEqualTo("C003");
    }

    @Test
    @DisplayName("UNAUTHORIZED - 401 상태")
    void unauthorized() {
        assertThat(CommonErrorCode.UNAUTHORIZED.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(CommonErrorCode.UNAUTHORIZED.getCode()).isEqualTo("C004");
    }

    @Test
    @DisplayName("ACCESS_DENIED - 403 상태")
    void accessDenied() {
        assertThat(CommonErrorCode.ACCESS_DENIED.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(CommonErrorCode.ACCESS_DENIED.getCode()).isEqualTo("C005");
    }

    @Test
    @DisplayName("RESOURCE_NOT_FOUND - 404 상태")
    void resourceNotFound() {
        assertThat(CommonErrorCode.RESOURCE_NOT_FOUND.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(CommonErrorCode.RESOURCE_NOT_FOUND.getCode()).isEqualTo("C006");
    }

    @Test
    @DisplayName("METHOD_NOT_ALLOWED - 405 상태")
    void methodNotAllowed() {
        assertThat(CommonErrorCode.METHOD_NOT_ALLOWED.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(CommonErrorCode.METHOD_NOT_ALLOWED.getCode()).isEqualTo("C007");
    }

    @Test
    @DisplayName("CONFLICT - 409 상태")
    void conflict() {
        assertThat(CommonErrorCode.CONFLICT.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(CommonErrorCode.CONFLICT.getCode()).isEqualTo("C008");
    }

    @Test
    @DisplayName("INTERNAL_SERVER_ERROR - 500 상태")
    void internalServerError() {
        assertThat(CommonErrorCode.INTERNAL_SERVER_ERROR.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode()).isEqualTo("C999");
    }

    @Test
    @DisplayName("ErrorCode 인터페이스 구현 확인")
    void implementsErrorCode() {
        for (CommonErrorCode code : CommonErrorCode.values()) {
            assertThat(code).isInstanceOf(ErrorCode.class);
            assertThat(code.getCode()).isNotBlank();
            assertThat(code.getMessage()).isNotBlank();
            assertThat(code.getStatus()).isNotNull();
        }
    }
}