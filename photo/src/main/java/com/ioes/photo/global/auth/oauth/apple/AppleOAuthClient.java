package com.ioes.photo.global.auth.oauth.apple;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ioes.photo.global.auth.oauth.OAuthClient;
import com.ioes.photo.global.auth.oauth.OAuthProvider;
import com.ioes.photo.global.auth.oauth.OAuthUserInfo;
import com.ioes.photo.global.config.oauth.properties.OAuthProperties;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

/**
 * Apple Sign In 네이티브 SDK 클라이언트 (전략 패턴 구현체).
 *
 * 네이티브 SDK 플로우:
 * 앱이 Apple SDK로 발급받은 identityToken(RS256 JWT)을 백엔드에 전달하면,
 * 백엔드가 Apple JWKS를 이용해 서명을 검증하고 사용자 정보를 추출합니다.
 *
 * params 키:
 * - identityToken: Apple SDK가 발급한 identity token JWT (필수)
 * - nickname: 최초 로그인 시 앱이 전달한 사용자 이름 (선택)
 *
 * @see OAuthProperties.Apple
 * @see ApplePublicKeyProvider
 * @author 황제연
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppleOAuthClient implements OAuthClient {

    private static final String APPLE_REVOKE_URL = "https://appleid.apple.com/auth/revoke";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OAuthProperties oAuthProperties;
    private final ApplePublicKeyProvider applePublicKeyProvider;

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.APPLE;
    }

    @Override
    public OAuthUserInfo getUserInfo(Map<String, String> params) {
        String identityToken = params.get("identityToken");
        String nickname = params.get("nickname");

        Claims claims = verifyIdentityToken(identityToken);
        String providerId = claims.getSubject();
        String email = claims.get("email", String.class);

        log.debug("Apple 사용자 인증 완료: providerId={}", providerId);
        return new OAuthUserInfo(providerId, email, nickname, null, OAuthProvider.APPLE, null);
    }

    @Override
    public void revokeConnection(String providerUserId, String providerRefreshToken) {
        if (providerRefreshToken == null || providerRefreshToken.isBlank()) {
            log.warn("Apple refresh token이 없어 연동 해제를 건너뜁니다: providerUserId={}", providerUserId);
            return;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", oAuthProperties.apple().clientId());
        form.add("client_secret", generateClientSecret());
        form.add("token", providerRefreshToken);
        form.add("token_type_hint", "refresh_token");

        restClient.post()
            .uri(APPLE_REVOKE_URL)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .toBodilessEntity();

        log.info("Apple 연동 해제 완료: providerUserId={}", providerUserId);
    }

    private Claims verifyIdentityToken(String identityToken) {
        String kid = extractKidFromHeader(identityToken);
        PublicKey publicKey = applePublicKeyProvider.getPublicKey(kid);

        try {
            Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(APPLE_ISSUER)
                .build()
                .parseSignedClaims(identityToken)
                .getPayload();

            if (!claims.getAudience().contains(oAuthProperties.apple().clientId())) {
                throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "Apple 토큰 audience 불일치");
            }
            return claims;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Apple identity token 검증 실패", e);
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "Apple 인증 토큰 검증 실패");
        }
    }

    private String extractKidFromHeader(String identityToken) {
        try {
            String[] parts = identityToken.split("\\.");
            if (parts.length < 2) {
                throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "유효하지 않은 JWT 형식");
            }
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            Map<String, Object> header = objectMapper.readValue(headerJson, new TypeReference<>() {});
            String kid = (String) header.get("kid");
            if (kid == null) {
                throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "Apple 토큰에 kid가 없습니다.");
            }
            return kid;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "Apple identity token 헤더 파싱 실패");
        }
    }

    private String generateClientSecret() {
        OAuthProperties.Apple apple = oAuthProperties.apple();
        try {
            PrivateKey privateKey = loadEcPrivateKey(apple.privateKey());
            Date now = new Date();

            return Jwts.builder()
                .header().add("kid", apple.keyId()).and()
                .issuer(apple.teamId())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + apple.clientSecretTtlMs()))
                .audience().add(APPLE_ISSUER).and()
                .subject(apple.clientId())
                .signWith(privateKey, Jwts.SIG.ES256)
                .compact();
        } catch (Exception e) {
            log.error("Apple client_secret 생성 실패", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "Apple 인증 설정 오류");
        }
    }

    private PrivateKey loadEcPrivateKey(String pem) throws Exception {
        String cleaned = pem.trim()
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(cleaned);
        return KeyFactory.getInstance("EC")
            .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }
}
