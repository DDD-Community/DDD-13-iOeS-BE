package com.ioes.photo.global.config.security;

import com.ioes.photo.domain.user.enums.UserRole;
import com.ioes.photo.global.auth.token.TokenService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * {@link JwtAuthenticationFilter} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter 단위 테스트")
class JwtAuthenticationFilterTest {

    @Mock JwtProvider jwtProvider;
    @Mock TokenService tokenService;
    @Mock FilterChain filterChain;

    @InjectMocks JwtAuthenticationFilter filter;

    private static final String TOKEN = "valid.access.token";

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void givenValidAccessToken(UserRole role) {
        given(jwtProvider.validateToken(TOKEN)).willReturn(true);
        given(jwtProvider.extractTokenType(TOKEN)).willReturn(JwtProvider.TOKEN_TYPE_ACCESS);
        given(tokenService.isBlacklisted(TOKEN)).willReturn(false);
        given(jwtProvider.extractSubject(TOKEN)).willReturn("1");
        given(jwtProvider.extractRole(TOKEN)).willReturn(role);
    }

    private MockHttpServletRequest requestWithBearer() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + TOKEN);
        return request;
    }

    @Test
    @DisplayName("USER_ADMIN 토큰이면 ROLE_USER_ADMIN authority를 설정한다")
    void shouldSetAdminAuthority() throws Exception {
        givenValidAccessToken(UserRole.USER_ADMIN);

        filter.doFilter(requestWithBearer(), new MockHttpServletResponse(), filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("1");
        assertThat(auth.getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_USER_ADMIN");
        then(filterChain).should().doFilter(any(), any());
    }

    @Test
    @DisplayName("USER_CUSTOMER 토큰이면 ROLE_USER_CUSTOMER authority를 설정한다")
    void shouldSetCustomerAuthority() throws Exception {
        givenValidAccessToken(UserRole.USER_CUSTOMER);

        filter.doFilter(requestWithBearer(), new MockHttpServletResponse(), filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_USER_CUSTOMER");
    }

    @Test
    @DisplayName("토큰이 없으면 인증 정보를 설정하지 않는다")
    void shouldNotAuthenticate_whenNoToken() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        then(filterChain).should().doFilter(any(), any());
    }
}
