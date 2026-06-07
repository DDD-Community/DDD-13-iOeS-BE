package com.ioes.photo.global.auth.oauth.apple;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Apple Sign In JWKS(공개키) 제공자.
 *
 * Apple의 공개키 엔드포인트에서 JWKS를 조회하고 1시간 단위로 캐싱합니다.
 * identity_token 서명 검증에 사용됩니다.
 *
 * @author 황제연
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplePublicKeyProvider {

    private static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";
    private static final long CACHE_TTL_MS = 60 * 60 * 1000L;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private volatile List<Map<String, String>> cachedKeys;
    private volatile long cacheExpireAt = 0L;

    public PublicKey getPublicKey(String kid) {
        List<Map<String, String>> keys = loadKeys();
        Map<String, String> jwk = keys.stream()
            .filter(k -> kid.equals(k.get("kid")))
            .findFirst()
            .orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED,
                "Apple 공개키를 찾을 수 없습니다: kid=" + kid));
        return buildRsaPublicKey(jwk);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> loadKeys() {
        if (cachedKeys != null && System.currentTimeMillis() < cacheExpireAt) {
            return cachedKeys;
        }
        synchronized (this) {
            if (cachedKeys != null && System.currentTimeMillis() < cacheExpireAt) {
                return cachedKeys;
            }
            try {
                String body = restClient.get()
                    .uri(APPLE_KEYS_URL)
                    .retrieve()
                    .body(String.class);
                Map<String, Object> parsed = objectMapper.readValue(body, new TypeReference<>() {});
                List<Map<String, String>> keys = (List<Map<String, String>>) parsed.get("keys");
                cachedKeys = keys;
                cacheExpireAt = System.currentTimeMillis() + CACHE_TTL_MS;
                log.debug("Apple JWKS 갱신 완료: {}개 키", keys.size());
                return keys;
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("Apple JWKS 조회 실패", e);
                throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "Apple 공개키 조회 실패");
            }
        }
    }

    private PublicKey buildRsaPublicKey(Map<String, String> jwk) {
        try {
            BigInteger modulus  = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.get("n")));
            BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.get("e")));
            return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
        } catch (Exception e) {
            log.error("Apple RSA 공개키 변환 실패", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "Apple 공개키 변환 실패");
        }
    }
}
