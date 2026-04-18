package com.ioes.photo.global.auth.oauth;

import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 등록된 OAuthClient 구현체를 관리하는 레지스트리
 *
 * Spring이 OAuthClient를 구현한 모든 빈을 자동으로 수집하므로
 * 새 공급자 구현체를등록하기만 하면 이 레지스트리가 별도 수정 없이 인식합니다
 *
 * @author 황제연
 */
@Slf4j
@Component
public class OAuthClientRegistry {

    private final Map<OAuthProvider, OAuthClient> clients;

    public OAuthClientRegistry(List<OAuthClient> clients) {
        this.clients = clients.stream()
            .collect(Collectors.toUnmodifiableMap(OAuthClient::getProvider, Function.identity()));
        log.info("등록된 OAuth 공급자: {}", this.clients.keySet());
    }

    public OAuthClient getClient(OAuthProvider provider) {
        OAuthClient client = clients.get(provider);
        if (client == null) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE,
                "지원하지 않는 OAuth 공급자입니다: " + provider);
        }
        return client;
    }
}