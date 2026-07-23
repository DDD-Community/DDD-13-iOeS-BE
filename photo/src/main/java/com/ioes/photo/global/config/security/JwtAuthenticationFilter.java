package com.ioes.photo.global.config.security;

import com.ioes.photo.global.auth.BearerTokenExtractor;
import com.ioes.photo.global.auth.token.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.ioes.photo.domain.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT Bearer 토큰을 검증하고 SecurityContextHolder에 인증 정보를 설정하는 필터.
 *
 * 토큰이 없거나 유효하지 않은 경우 인증 없이 다음 필터로 진행합니다.
 * 인증 필요 경로에서 인증 실패 처리는 Spring Security의 ExceptionTranslationFilter가 담당합니다.
 *
 * @see JwtProvider
 * @author 황제연
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = BearerTokenExtractor.extract(request);

        if (token != null && jwtProvider.validateToken(token) && isAccessToken(token) && !tokenService.isBlacklisted(token)) {
            String subject = jwtProvider.extractSubject(token);
            UserRole role = jwtProvider.extractRole(token);
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(subject, null,
                    List.of(new SimpleGrantedAuthority(role.authority())));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAccessToken(String token) {
        return JwtProvider.TOKEN_TYPE_ACCESS.equals(jwtProvider.extractTokenType(token));
    }

}