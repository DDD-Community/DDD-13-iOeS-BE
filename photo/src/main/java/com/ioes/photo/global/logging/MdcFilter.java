package com.ioes.photo.global.logging;

import com.ioes.photo.global.config.mdc.properties.MdcProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 모든 HTTP 요청에 MDC(Mapped Diagnostic Context) 컨텍스트를 설정하는 필터.
 *
 * 요청마다 고유한 requestId(UUID), 요청 URI, HTTP 메서드, 클라이언트 IP,
 * 인증된 사용자 ID를 MDC에 저장하여 logback 로그에 자동으로 포함시킵니다.
 * 요청 처리가 완료되면 finally 블록에서 clear()를 호출하여 ThreadLocal 메모리 누수를 방지합니다.
 *
 * Order(-90):  Spring Security FilterChainProxy(-100) 이후 실행되므로
 * SecurityContextHolder에서
 * 인증 정보를 읽을 수 있습니다.
 *
 * @see MdcProperties
 * @see MdcTaskDecorator
 * @author 황제연
 */
@Slf4j
@Component
@Order(-90)
@RequiredArgsConstructor
public class MdcFilter extends OncePerRequestFilter {

    private final MdcProperties mdcProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            MdcProperties.Keys keys = mdcProperties.keys();
            MDC.put(keys.requestId(), UUID.randomUUID().toString());
            MDC.put(keys.requestUri(), request.getRequestURI());
            MDC.put(keys.method(), request.getMethod());
            MDC.put(keys.clientIp(), extractClientIp(request));
            setUserIdIfAuthenticated(keys.userId());

            long start = System.currentTimeMillis();
            chain.doFilter(request, response);
            long duration = System.currentTimeMillis() - start;

            if (!mdcProperties.isRequestLogExcluded(request.getRequestURI())) {
                log.info("{} {} {} {}ms",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
            }
        } finally {
            MDC.clear();
        }
    }

    private void setUserIdIfAuthenticated(String userIdKey) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            MDC.put(userIdKey, auth.getName());
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader(mdcProperties.forwardedIpHeader());
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
