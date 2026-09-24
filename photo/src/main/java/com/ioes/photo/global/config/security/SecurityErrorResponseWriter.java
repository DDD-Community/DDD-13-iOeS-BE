package com.ioes.photo.global.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ioes.photo.global.common.response.ApiResponse;
import com.ioes.photo.global.error.code.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * 시큐리티 필터체인에서 끊긴 요청의 응답 본문을 기록한다.
 *
 * 필터 단계의 거부는 DispatcherServlet에 도달하지 못해 GlobalExceptionHandler를 타지 않는다.
 * 컨테이너 기본 오류 페이지 대신 이 클래스로 ApiResponse 포맷을 맞춰,
 * 클라이언트가 응답 위치와 무관하게 동일한 스키마로 에러를 파싱할 수 있게 한다.
 *
 * @author 황제연
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode));
    }
}
