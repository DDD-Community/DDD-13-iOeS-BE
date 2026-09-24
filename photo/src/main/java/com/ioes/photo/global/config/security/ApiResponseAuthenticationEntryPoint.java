package com.ioes.photo.global.config.security;

import com.ioes.photo.global.error.code.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 인증되지 않은 요청에 대한 401 응답 진입점.
 *
 * 비로그인 사용자가 인증이 필요한 API를 호출하면 ApiResponse 포맷의 C004로 응답한다.
 *
 * @author 황제연
 */
@Component
@RequiredArgsConstructor
public class ApiResponseAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorResponseWriter responseWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        responseWriter.write(response, CommonErrorCode.UNAUTHORIZED);
    }
}
