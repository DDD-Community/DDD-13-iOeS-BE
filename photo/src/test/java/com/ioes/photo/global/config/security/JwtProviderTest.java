package com.ioes.photo.global.config.security;

import com.ioes.photo.domain.user.enums.UserRole;
import com.ioes.photo.global.config.security.properties.JwtProperties;
import com.ioes.photo.global.config.security.properties.TokenProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JwtProvider} 권한 claim 관련 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("JwtProvider 단위 테스트")
class JwtProviderTest {

    // 32바이트 이상 Base64URL 인코딩 시크릿
    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtcHJvdmlkZXItdW5pdC10ZXN0LTEyMzQ1Ng";
    private static final long EXPIRATION_MS = 3_600_000L;

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties(SECRET, EXPIRATION_MS);
        TokenProperties tokenProperties =
            new TokenProperties(0L, "refresh:", "user_tokens:", "provider_rt:", "blacklist:", 300_000L);
        jwtProvider = new JwtProvider(jwtProperties, tokenProperties);
        jwtProvider.init();
    }

    @Test
    @DisplayName("생성한 토큰에서 role claim을 추출한다")
    void shouldExtractRole() {
        String token = jwtProvider.generateToken("1", UserRole.USER_ADMIN);

        assertThat(jwtProvider.extractRole(token)).isEqualTo(UserRole.USER_ADMIN);
        assertThat(jwtProvider.extractSubject(token)).isEqualTo("1");
    }

    @Test
    @DisplayName("USER_CUSTOMER role도 정상 추출한다")
    void shouldExtractCustomerRole() {
        String token = jwtProvider.generateToken("2", UserRole.USER_CUSTOMER);

        assertThat(jwtProvider.extractRole(token)).isEqualTo(UserRole.USER_CUSTOMER);
    }

    @Test
    @DisplayName("role claim이 없는 토큰(배포 전 발급분)은 USER_CUSTOMER로 폴백한다")
    void shouldFallbackToCustomer_whenNoRoleClaim() {
        String tokenWithoutRole = Jwts.builder()
            .subject("3")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
            .signWith(secretKey())
            .compact();

        assertThat(jwtProvider.extractRole(tokenWithoutRole)).isEqualTo(UserRole.USER_CUSTOMER);
    }

    @Test
    @DisplayName("알 수 없는 role 값이면 USER_CUSTOMER로 폴백한다")
    void shouldFallbackToCustomer_whenUnknownRole() {
        String tokenWithBadRole = Jwts.builder()
            .subject("4")
            .claim(JwtProvider.ROLE_CLAIM, "SUPER_ADMIN")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
            .signWith(secretKey())
            .compact();

        assertThat(jwtProvider.extractRole(tokenWithBadRole)).isEqualTo(UserRole.USER_CUSTOMER);
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(SECRET));
    }
}
