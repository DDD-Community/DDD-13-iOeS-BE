package com.ioes.photo.global.config.security;

import com.ioes.photo.global.config.security.properties.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정 클래스.
 *
 * @see SecurityProperties
 * @see JwtAuthenticationFilter
 * @author 황제연
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityProperties securityProperties;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 보안 필터 체인을 구성합니다.
     *
     * @param http {@link HttpSecurity} 설정 객체
     * @return 구성된 {@link SecurityFilterChain}
     * @throws Exception 보안 설정 중 발생하는 예외
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        String[] publicUrls = securityProperties.publicUrls().toArray(String[]::new);

        http
            .cors(AbstractHttpConfigurer::disable)
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(publicUrls).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt 알고리즘을 사용하는 패스워드 인코더 빈을 등록합니다.
     *
     * @return {@link BCryptPasswordEncoder} 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * JWT 전용 인증 앱에서는 UserDetailsService를 사용하지 않습니다.
     * 이 빈을 등록하면 UserDetailsServiceAutoConfiguration이 비활성화되어
     * 서버 시작 시 임시 비밀번호가 생성되지 않습니다.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> { throw new UsernameNotFoundException("JWT 인증만 지원합니다."); };
    }
}