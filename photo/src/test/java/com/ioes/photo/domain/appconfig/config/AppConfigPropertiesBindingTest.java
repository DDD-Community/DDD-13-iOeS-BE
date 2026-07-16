package com.ioes.photo.domain.appconfig.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AppConfigProperties} yaml 바인딩 테스트.
 * 플랫폼별 중첩 구조가 프로퍼티로부터 정상 바인딩되는지 확인합니다.
 *
 * @author 김성민
 */
@SpringBootTest
@DisplayName("AppConfigProperties 바인딩 테스트")
class AppConfigPropertiesBindingTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () -> "c2VjcmV0LWtleS1mb3ItdGVzdGluZy1wdXJwb3Nlcy1vbmx5LW11c3QtYmUtYXQtbGVhc3QtNjQtYnl0ZXMtbG9uZw==");
    }

    @MockitoBean
    RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Test
    @DisplayName("iOS 설정이 바인딩된다")
    void shouldBindIosConfig() {
        AppConfigProperties.PlatformConfig ios = appConfigProperties.ios();

        assertThat(ios).isNotNull();
        assertThat(ios.minimumVersion()).isNotBlank();
        assertThat(ios.latestVersion()).isNotBlank();
        assertThat(ios.storeUrl()).contains("apps.apple.com");
        assertThat(ios.supportEmail()).isNotBlank();
        assertThat(ios.termsPolicies()).isNotEmpty();
        assertThat(ios.termsPolicies()).allSatisfy(policy -> {
            assertThat(policy.type()).isNotBlank();
            assertThat(policy.title()).isNotBlank();
            assertThat(policy.url()).isNotBlank();
        });
    }

    @Test
    @DisplayName("Android 설정이 바인딩된다")
    void shouldBindAndroidConfig() {
        AppConfigProperties.PlatformConfig android = appConfigProperties.android();

        assertThat(android).isNotNull();
        assertThat(android.minimumVersion()).isNotBlank();
        assertThat(android.latestVersion()).isNotBlank();
        assertThat(android.storeUrl()).contains("play.google.com");
        assertThat(android.supportEmail()).isNotBlank();
        assertThat(android.termsPolicies()).isNotEmpty();
        assertThat(android.termsPolicies()).allSatisfy(policy -> {
            assertThat(policy.type()).isNotBlank();
            assertThat(policy.title()).isNotBlank();
            assertThat(policy.url()).isNotBlank();
        });
    }
}
