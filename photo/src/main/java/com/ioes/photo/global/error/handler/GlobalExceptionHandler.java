package com.ioes.photo.global.error.handler;

import com.ioes.photo.domain.user.dto.AccountDeletedResponse;
import com.ioes.photo.global.common.response.ApiResponse;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.AccountDeletedException;
import com.ioes.photo.global.error.exception.BusinessException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

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

    /**
     * 콘텐츠 협상 실패(406).
     *
     * <p>클라이언트의 {@code Accept} 헤더가 서버 표현과 맞지 않는 경우로,
     * 응답 바디를 다시 직렬화하려다 재차 협상에 실패하는 것을 피하기 위해 상태코드만 반환한다.
     * (Spring {@code ResponseEntityExceptionHandler}의 처리와 동일)
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Void> handleHttpMediaTypeNotAcceptableException(HttpMediaTypeNotAcceptableException e) {
        log.warn("HttpMediaTypeNotAcceptableException: {}", e.getMessage());
        return ResponseEntity.status(CommonErrorCode.NOT_ACCEPTABLE.getStatus()).build();
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.warn("HttpMediaTypeNotSupportedException: {}", e.getMessage());
        return ResponseEntity
            .status(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.getStatus())
            .body(ApiResponse.error(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());
        return ResponseEntity
            .status(CommonErrorCode.INVALID_INPUT_VALUE.getStatus())
            .body(ApiResponse.error(CommonErrorCode.INVALID_INPUT_VALUE));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestPartException(MissingServletRequestPartException e) {
        log.warn("MissingServletRequestPartException: {}", e.getMessage());
        return ResponseEntity
            .status(CommonErrorCode.INVALID_INPUT_VALUE.getStatus())
            .body(ApiResponse.error(CommonErrorCode.INVALID_INPUT_VALUE, e.getMessage()));
    }

    /**
     * 업로드 용량 초과(413). {@link MaxUploadSizeExceededException}은
     * {@link MultipartException}의 하위 타입이므로 더 구체적인 이 핸들러가 우선 매칭된다.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("MaxUploadSizeExceededException: {}", e.getMessage());
        return ResponseEntity
            .status(CommonErrorCode.PAYLOAD_TOO_LARGE.getStatus())
            .body(ApiResponse.error(CommonErrorCode.PAYLOAD_TOO_LARGE));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMultipartException(MultipartException e) {
        log.warn("MultipartException: {}", e.getMessage());
        return ResponseEntity
            .status(CommonErrorCode.INVALID_INPUT_VALUE.getStatus())
            .body(ApiResponse.error(CommonErrorCode.INVALID_INPUT_VALUE));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidationException(HandlerMethodValidationException e) {
        log.warn("HandlerMethodValidationException: {}", e.getMessage());
        return ResponseEntity
            .status(CommonErrorCode.INVALID_INPUT_VALUE.getStatus())
            .body(ApiResponse.error(CommonErrorCode.INVALID_INPUT_VALUE));
    }

    /**
     * {@link ConstraintViolationException}보다 상위인 {@link ValidationException} 안전망.
     *
     * <p>{@code NaN}/{@code Infinity} 값이 {@code @Digits} 검증 내부에서
     * {@code new BigDecimal("NaN")}으로 실패할 때 던져지는 {@code ValidationException}(HV000028) 등,
     * 표준 검증 예외가 500으로 새어 나가는 것을 막는다.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(ValidationException e) {
        log.warn("ValidationException: {}", e.getMessage());
        return ResponseEntity
            .status(CommonErrorCode.INVALID_INPUT_VALUE.getStatus())
            .body(ApiResponse.error(CommonErrorCode.INVALID_INPUT_VALUE));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("IllegalArgumentException: {}", e.getMessage());
        return ResponseEntity
            .status(CommonErrorCode.INVALID_INPUT_VALUE.getStatus())
            .body(ApiResponse.error(CommonErrorCode.INVALID_INPUT_VALUE));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity
            .status(CommonErrorCode.INTERNAL_SERVER_ERROR.getStatus())
            .body(ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR));
    }
}