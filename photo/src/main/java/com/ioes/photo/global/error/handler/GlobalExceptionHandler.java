package com.ioes.photo.global.error.handler;

import com.ioes.photo.domain.user.dto.AccountDeletedResponse;
import com.ioes.photo.global.common.response.ApiResponse;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.AccountDeletedException;
import com.ioes.photo.global.error.exception.BusinessException;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 애플리케이션 전역 예외 처리 핸들러.
 *
 * @author 황제연
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountDeletedException.class)
    public ResponseEntity<ApiResponse<AccountDeletedResponse>> handleAccountDeletedException(AccountDeletedException e) {
        log.warn("AccountDeletedException: {}", e.getMessage());
        return ResponseEntity
            .status(e.getErrorCode().getStatus())
            .body(ApiResponse.error(e.getErrorCode(), e.getMessage(), new AccountDeletedResponse(e.getRestoreToken())));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());
        return ResponseEntity
            .status(e.getErrorCode().getStatus())
            .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        log.warn("MethodArgumentNotValidException: {}", message);
        return ResponseEntity
            .status(CommonErrorCode.INVALID_INPUT_VALUE.getStatus())
            .body(ApiResponse.error(CommonErrorCode.INVALID_INPUT_VALUE, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("ConstraintViolationException: {}", e.getMessage());
        return ResponseEntity
            .status(CommonErrorCode.INVALID_INPUT_VALUE.getStatus())
            .body(ApiResponse.error(CommonErrorCode.INVALID_INPUT_VALUE, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("MethodArgumentTypeMismatchException: {}", e.getMessage());
        return ResponseEntity
            .status(CommonErrorCode.INVALID_TYPE_VALUE.getStatus())
            .body(ApiResponse.error(CommonErrorCode.INVALID_TYPE_VALUE));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("MissingServletRequestParameterException: {}", e.getMessage());
        return ResponseEntity
            .status(CommonErrorCode.MISSING_REQUEST_PARAMETER.getStatus())
            .body(ApiResponse.error(CommonErrorCode.MISSING_REQUEST_PARAMETER, e.getMessage()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("HttpRequestMethodNotSupportedException: {}", e.getMessage());
        return ResponseEntity
            .status(CommonErrorCode.METHOD_NOT_ALLOWED.getStatus())
            .body(ApiResponse.error(CommonErrorCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException e) {
        log.warn("AuthenticationException: {}", e.getMessage());
        return ResponseEntity
            .status(CommonErrorCode.UNAUTHORIZED.getStatus())
            .body(ApiResponse.error(CommonErrorCode.UNAUTHORIZED, e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("AccessDeniedException: {}", e.getMessage());
        return ResponseEntity
            .status(CommonErrorCode.ACCESS_DENIED.getStatus())
            .body(ApiResponse.error(CommonErrorCode.ACCESS_DENIED));
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFoundException(Exception e) {
        log.warn("NoHandlerFound: {}", e.getMessage());
        return ResponseEntity
            .status(CommonErrorCode.RESOURCE_NOT_FOUND.getStatus())
            .body(ApiResponse.error(CommonErrorCode.RESOURCE_NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity
            .status(CommonErrorCode.INTERNAL_SERVER_ERROR.getStatus())
            .body(ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR));
    }
}