package com.ioes.photo.global.config.security;

import com.ioes.photo.global.error.code.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 인증은 됐으나 권한이 부족한 요청에 대한 403 응답 핸들러.
 *
 * 메서드 보안(@AdminOnly)의 거부는 GlobalExceptionHandler가 처리하지만,
 * 필터체인 단계의 거부는 여기서 동일한 ApiResponse 포맷으로 맞춘다.
 *
 * @author 황제연
 */
@Component
@RequiredArgsConstructor
public class ApiResponseAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorResponseWriter responseWriter;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        responseWriter.write(response, CommonErrorCode.ACCESS_DENIED);
    }
}
