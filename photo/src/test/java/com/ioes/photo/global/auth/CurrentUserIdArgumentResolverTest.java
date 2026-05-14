package com.ioes.photo.global.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CurrentUserIdArgumentResolver 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("CurrentUserIdArgumentResolver 단위 테스트")
class CurrentUserIdArgumentResolverTest {

    private final CurrentUserIdArgumentResolver resolver = new CurrentUserIdArgumentResolver();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── supportsParameter ─────────────────────────────────────────────────────

    @Test
    @DisplayName("@CurrentUserId Long 파라미터는 지원한다")
    void supports_currentUserIdLong() throws Exception {
        MethodParameter param = methodParam(TestController.class, "withUserId", Long.class);

        assertThat(resolver.supportsParameter(param)).isTrue();
    }

    @Test
    @DisplayName("@CurrentUserId 없는 Long 파라미터는 지원하지 않는다")
    void notSupports_longWithoutAnnotation() throws Exception {
        MethodParameter param = methodParam(TestController.class, "withoutAnnotation", Long.class);

        assertThat(resolver.supportsParameter(param)).isFalse();
    }

    @Test
    @DisplayName("@CurrentUserId String 파라미터는 지원하지 않는다")
    void notSupports_currentUserIdString() throws Exception {
        MethodParameter param = methodParam(TestController.class, "withString", String.class);

        assertThat(resolver.supportsParameter(param)).isFalse();
    }

    // ── resolveArgument ───────────────────────────────────────────────────────

    @Test
    @DisplayName("인증된 사용자면 JWT subject를 파싱한 userId를 반환한다")
    void resolvesUserId_whenAuthenticated() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("42", null, List.of())
        );

        Object result = resolver.resolveArgument(null, null, null, null);

        assertThat(result).isEqualTo(42L);
    }

    @Test
    @DisplayName("AnonymousAuthenticationToken이면 null을 반환한다")
    void resolvesNull_whenAnonymous() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
            new AnonymousAuthenticationToken("key", "anonymous",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")))
        );

        Object result = resolver.resolveArgument(null, null, null, null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Authentication이 null이면 null을 반환한다")
    void resolvesNull_whenNoAuthentication() throws Exception {
        SecurityContextHolder.clearContext();

        Object result = resolver.resolveArgument(null, null, null, null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("userId가 Long으로 파싱 불가능한 토큰이면 BadCredentialsException을 던진다")
    void throwsBadCredentials_whenNameIsNotLong() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("not-a-number", null, List.of())
        );

        assertThatThrownBy(() -> resolver.resolveArgument(null, null, null, null))
            .isInstanceOf(BadCredentialsException.class);
    }

    // ── helper ──────────────────────────────────────────────────────────────

    private static MethodParameter methodParam(Class<?> clazz, String methodName, Class<?>... paramTypes) throws Exception {
        Method method = clazz.getDeclaredMethod(methodName, paramTypes);
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    static class TestController {
        void withUserId(@CurrentUserId Long userId) {}
        void withoutAnnotation(Long userId) {}
        void withString(@CurrentUserId String userId) {}
    }
}
