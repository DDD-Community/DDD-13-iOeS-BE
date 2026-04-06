package com.ioes.photo.global.auth.oauth;

import com.ioes.photo.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link OAuthClientRegistry} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("OAuthClientRegistry 단위 테스트")
class OAuthClientRegistryTest {

    @Test
    @DisplayName("등록된 공급자에 해당하는 OAuthClient를 반환한다")
    void shouldReturnRegisteredClient() {
        OAuthClient appleClient = stubClient(OAuthProvider.APPLE);
        OAuthClient kakaoClient = stubClient(OAuthProvider.KAKAO);

        OAuthClientRegistry registry = new OAuthClientRegistry(List.of(appleClient, kakaoClient));

        assertThat(registry.getClient(OAuthProvider.APPLE)).isSameAs(appleClient);
        assertThat(registry.getClient(OAuthProvider.KAKAO)).isSameAs(kakaoClient);
    }

    @Test
    @DisplayName("등록되지 않은 공급자이면 BusinessException을 던진다")
    void shouldThrow_whenProviderNotRegistered() {
        OAuthClient kakaoClient = stubClient(OAuthProvider.KAKAO);
        OAuthClientRegistry registry = new OAuthClientRegistry(List.of(kakaoClient));

        assertThatThrownBy(() -> registry.getClient(OAuthProvider.APPLE))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("빈 목록으로 생성 후 조회하면 BusinessException을 던진다")
    void shouldThrow_whenRegistryIsEmpty() {
        OAuthClientRegistry registry = new OAuthClientRegistry(List.of());

        assertThatThrownBy(() -> registry.getClient(OAuthProvider.KAKAO))
            .isInstanceOf(BusinessException.class);
    }

    // ── helper ───────────────────────────────────────────────────────────

    private OAuthClient stubClient(OAuthProvider provider) {
        OAuthClient client = mock(OAuthClient.class);
        when(client.getProvider()).thenReturn(provider);
        return client;
    }
}