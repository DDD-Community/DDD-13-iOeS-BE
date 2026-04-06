package com.ioes.photo.global.config.security;

import com.ioes.photo.global.config.security.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 토큰 생성·검증·파싱 유틸리티.
 * HMAC-SHA256 알고리즘으로 서명합니다
 * 비밀 키는 Base64 디코딩하여 사용합니다.
 *
 *
 * @see JwtProperties
 * @see JwtAuthenticationFilter
 * @author 황제연
 */
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties jwtProperties;
    private SecretKey cachedSecretKey;

    @PostConstruct
    void init() {
        this.cachedSecretKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(jwtProperties.secret()));
    }

    /**
     * subject(사용자 식별자)를 담은 JWT 토큰을 생성합니다.
     *
     * @param subject 토큰에 저장할 사용자 식별자
     * @return 서명된 JWT 토큰 문자열
     */
    public String generateToken(String subject) {
        Date now = new Date();
        return Jwts.builder()
            .subject(subject)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + jwtProperties.expirationMs()))
            .signWith(secretKey())
            .compact();
    }

    /**
     * 토큰에서 subject를 추출합니다.
     *
     * @param token JWT 토큰 문자열
     * @return 토큰의 subject (사용자 식별자)
     * @throws JwtException 토큰이 유효하지 않거나 만료된 경우
     */
    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 토큰의 서명 및 만료 여부를 검증합니다.
     *
     * @param token JWT 토큰 문자열
     * @return 유효하면 {@code true}, 서명 불일치·만료·형식 오류면 {@code false}
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(secretKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey secretKey() {
        return cachedSecretKey;
    }
}