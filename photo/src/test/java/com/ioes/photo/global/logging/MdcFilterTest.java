package com.ioes.photo.global.logging;

import com.ioes.photo.global.config.mdc.properties.MdcProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MdcFilter} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("MdcFilter 테스트")
class MdcFilterTest {

    private MdcFilter mdcFilter;
    private MdcProperties.Keys keys;

    @BeforeEach
    void setUp() {
        keys = new MdcProperties.Keys("requestId", "userId", "uri", "method", "clientIp");
        MdcProperties props = new MdcProperties("X-Forwarded-For", keys);
        mdcFilter = new MdcFilter(props);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("요청 처리 중 MDC에 requestId, uri, method, clientIp 설정됨")
    void setsMdcContextDuringRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.setRemoteAddr("192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] capturedRequestId = {null};
        final String[] capturedUri = {null};
        final String[] capturedMethod = {null};
        final String[] capturedIp = {null};

        FilterChain chain = (req, res) -> {
            capturedRequestId[0] = MDC.get(keys.requestId());
            capturedUri[0] = MDC.get(keys.requestUri());
            capturedMethod[0] = MDC.get(keys.method());
            capturedIp[0] = MDC.get(keys.clientIp());
        };

        mdcFilter.doFilter(request, response, chain);

        assertThat(capturedRequestId[0]).isNotBlank();
        assertThat(capturedUri[0]).isEqualTo("/api/users");
        assertThat(capturedMethod[0]).isEqualTo("GET");
        assertThat(capturedIp[0]).isEqualTo("192.168.1.1");
    }

    @Test
    @DisplayName("요청 완료 후 MDC가 초기화됨")
    void clearsMdcAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        mdcFilter.doFilter(request, response, chain);

        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    @DisplayName("X-Forwarded-For 헤더가 있으면 해당 IP 사용")
    void usesForwardedIpHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] capturedIp = {null};
        FilterChain chain = (req, res) -> capturedIp[0] = MDC.get(keys.clientIp());

        mdcFilter.doFilter(request, response, chain);

        assertThat(capturedIp[0]).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("인증된 사용자의 userId가 MDC에 설정됨")
    void setsUserIdForAuthenticatedUser() throws Exception {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("user123", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] capturedUserId = {null};
        FilterChain chain = (req, res) -> capturedUserId[0] = MDC.get(keys.userId());

        mdcFilter.doFilter(request, response, chain);

        assertThat(capturedUserId[0]).isEqualTo("user123");
    }

    @Test
    @DisplayName("미인증 요청에서는 userId가 MDC에 설정되지 않음")
    void doesNotSetUserId_forAnonymousRequest() throws Exception {
        SecurityContextHolder.clearContext();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/public");
        MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] capturedUserId = {null};
        FilterChain chain = (req, res) -> capturedUserId[0] = MDC.get(keys.userId());

        mdcFilter.doFilter(request, response, chain);

        assertThat(capturedUserId[0]).isNull();
    }
}