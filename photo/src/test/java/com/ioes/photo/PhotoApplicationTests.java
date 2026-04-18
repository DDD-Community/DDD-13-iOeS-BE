package com.ioes.photo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 애플리케이션 컨텍스트 통합 테스트.
 * 모든 Bean이 정상적으로 생성되고 컨텍스트가 로드되는지 확인합니다.
 *
 * @author 황제연
 */
@SpringBootTest
@DisplayName("PhotoApplication 통합 테스트")
class PhotoApplicationTests {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () -> "c2VjcmV0LWtleS1mb3ItdGVzdGluZy1wdXJwb3Nlcy1vbmx5LW11c3QtYmUtYXQtbGVhc3QtNjQtYnl0ZXMtbG9uZw==");
    }

    /**
     * 실제 Redis 없이 컨텍스트 로드가 가능하도록 ConnectionFactory를 Mock 처리.
     * Spring Boot는 LettuceConnectionFactory 하나로 두 타입을 모두 제공하므로
     * 테스트에서도 각 타입별 별도 Mock이 필요합니다.
     */
    @MockitoBean
    RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @Test
    @DisplayName("Spring 애플리케이션 컨텍스트 정상 로드")
    void contextLoads() {
        // Spring 컨텍스트 로드 성공 여부만 확인
    }
}