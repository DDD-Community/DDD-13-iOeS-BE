package com.ioes.photo.global.error.handler;

import com.ioes.photo.global.common.response.ApiResponse;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link GlobalExceptionHandler} 단위 테스트.
 *
 * <p>Spring 컨텍스트 없이 핸들러 메서드를 직접 호출하여 응답 코드와 바디를 검증합니다.
 *
 * @author 황제연
 */
@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("BusinessException - 에러 코드에 맞는 HTTP 상태 반환")
    void handleBusinessException() {
        BusinessException ex = new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getCode()).isEqualTo("C006");
    }

    @Test
    @DisplayName("BusinessException - 커스텀 메시지 반영")
    void handleBusinessException_customMessage() {
        BusinessException ex = new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "이메일 형식 오류");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("이메일 형식 오류");
    }

    @Test
    @DisplayName("ConstraintViolationException - 400 BAD_REQUEST 반환")
    void handleConstraintViolationException() {
        ConstraintViolationException ex = new ConstraintViolationException("제약 위반", Set.of());

        ResponseEntity<ApiResponse<Void>> response = handler.handleConstraintViolationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo("C001");
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException - 400 타입 오류 반환")
    void handleMethodArgumentTypeMismatchException() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getMessage()).thenReturn("타입 불일치");

        ResponseEntity<ApiResponse<Void>> response = handler.handleMethodArgumentTypeMismatchException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo("C002");
    }

    @Test
    @DisplayName("MissingServletRequestParameterException - 400 파라미터 누락 반환")
    void handleMissingServletRequestParameterException() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("userId", "Long");

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleMissingServletRequestParameterException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo("C003");
    }

    @Test
    @DisplayName("HttpRequestMethodNotSupportedException - 405 반환")
    void handleHttpRequestMethodNotSupportedException() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("DELETE");

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleHttpRequestMethodNotSupportedException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().getCode()).isEqualTo("C007");
    }

    @Test
    @DisplayName("AccessDeniedException - 403 반환")
    void handleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("접근 거부");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDeniedException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getCode()).isEqualTo("C005");
    }

    @Test
    @DisplayName("Exception (예상 외 예외) - 500 반환")
    void handleException() {
        Exception ex = new RuntimeException("알 수 없는 에러");

        ResponseEntity<ApiResponse<Void>> response = handler.handleException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getCode()).isEqualTo("C999");
        assertThat(response.getBody().isSuccess()).isFalse();
    }
}