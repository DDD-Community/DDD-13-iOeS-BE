package com.ioes.photo.global.auth.oauth.apple;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ioes.photo.global.auth.oauth.OAuthClient;
import com.ioes.photo.global.auth.oauth.OAuthProvider;
import com.ioes.photo.global.auth.oauth.OAuthUserInfo;
import com.ioes.photo.global.config.oauth.properties.OAuthProperties;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
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
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

/**
 * Apple Sign In OAuth 클라이언트 - 전략패턴 구현체)
 *
 * - Apple의 인증 흐름:
 * 1. 인증 URL 생성 -> 사용자를 Apple 로그인 페이지로 리다이렉트
 * 2. Apple이 response_mode=form_post로 code를 POST 전송
 * 3. code와 동적으로 생성한 client_secret(JWT)으로 Apple 토큰 엔드포인트 호출
 * 4. ID token(JWT)에서 사용자 정보 파싱
 *
 *
 * 콜백 파라미터:
 * - code: Apple 인증 코드 (필수)
 * - user: 최초 로그인 시 Apple이 전달하는 사용자 정보 JSON (선택)
 *
 *
 * @see OAuthProperties.Apple
 * @author 황제연
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppleOAuthClient implements OAuthClient {

    private static final String APPLE_AUTH_URL = "https://appleid.apple.com/auth/authorize";
    private static final String APPLE_TOKEN_URL = "https://appleid.apple.com/auth/token";
    private static final String APPLE_AUDIENCE = "https://appleid.apple.com";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OAuthProperties oAuthProperties;

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.APPLE;
    }

    @Override
    public String getAuthorizationUrl() {
        OAuthProperties.Apple apple = oAuthProperties.apple();
        return APPLE_AUTH_URL + "?response_type=code" + "&client_id=" + apple.clientId()
            + "&redirect_uri=" + apple.redirectUri() + "&scope=name%20email" + "&response_mode=form_post";
    }

    @Override
    public OAuthUserInfo getUserInfo(Map<String, String> params) {
        String code = params.get("code");
        String userJson = params.get("user");

        AppleTokenResponse tokenResponse = exchangeCodeForToken(code);
        Map<String, Object> claims = parseIdTokenClaims(tokenResponse.idToken());

        String providerId = (String) claims.get("sub");
        String email = (String) claims.get("email");
        String nickname = extractNicknameFromUserJson(userJson);

        log.debug("Apple 사용자 정보 조회 완료: providerId={}", providerId);
        return new OAuthUserInfo(providerId, email, nickname, null, OAuthProvider.APPLE);
    }

    private AppleTokenResponse exchangeCodeForToken(String code) {
        OAuthProperties.Apple apple = oAuthProperties.apple();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", apple.clientId());
        form.add("client_secret", generateClientSecret());
        form.add("code", code);
        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", apple.redirectUri());

        return restClient.post()
            .uri(APPLE_TOKEN_URL)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(AppleTokenResponse.class);
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
                .audience().add(APPLE_AUDIENCE).and()
                .subject(apple.clientId())
                .signWith(privateKey, Jwts.SIG.ES256)
                .compact();
        } catch (Exception e) {
            log.error("Apple client_secret 생성 실패", e);
            // 이후 별도 인증관련 예외처리 공통화 진행할 예정
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

    private Map<String, Object> parseIdTokenClaims(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("유효하지 않은 JWT 형식");
            }
            String claimsJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            return objectMapper.readValue(claimsJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Apple ID token 파싱 실패", e);
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "Apple 인증 토큰 파싱 실패");
        }
    }

    @SuppressWarnings("unchecked")
    private String extractNicknameFromUserJson(String userJson) {
        if (userJson == null || userJson.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> userMap = objectMapper.readValue(userJson, new TypeReference<>() {});
            Map<String, Object> name    = (Map<String, Object>) userMap.get("name");
            if (name == null) return null;
            String firstName = (String) name.get("firstName");
            String lastName  = (String) name.get("lastName");
            if (firstName == null && lastName == null) return null;
            return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
        } catch (Exception e) {
            log.warn("Apple user JSON 파싱 실패: {}", userJson);
            return null;
        }
    }
}