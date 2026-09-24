package com.ioes.photo.global.config.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.ioes.photo.global.storage.S3StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 인증/인가 실패 응답 포맷 통합 테스트.
 *
 * 필터체인에서 끊긴 요청도 본문 응답과 동일한 ApiResponse 스키마로 내려가는지 확인한다.
 *
 * @author 황제연
 */
@SpringBootTest
@DisplayName("인증 실패 응답 포맷 통합 테스트")
class SecurityErrorResponseIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () ->
            "c2VjcmV0LWtleS1mb3ItdGVzdGluZy1wdXJwb3Nlcy1vbmx5LW11c3QtYmUtYXQtbGVhc3QtNjQtYnl0ZXMtbG9uZw==");
    }

    @MockitoBean RedisConnectionFactory redisConnectionFactory;
    @MockitoBean ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;
    @MockitoBean S3StorageService storageService;

    @Autowired WebApplicationContext webApplicationContext;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
    }

    @Test
    @DisplayName("비로그인 상태로 좋아요를 누르면 ApiResponse 포맷의 401(C004)로 응답한다")
    void anonymousLikeReturnsUnauthorizedApiResponse() throws Exception {
        mockMvc.perform(post("/v1/spots/1/likes"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("C004"))
            .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    @DisplayName("비로그인 상태로 좋아요를 취소해도 401(C004)로 응답한다")
    void anonymousUnlikeReturnsUnauthorizedApiResponse() throws Exception {
        mockMvc.perform(delete("/v1/spots/1/likes"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("C004"));
    }

    @Test
    @DisplayName("비로그인 상태로 나만의 스팟 목록을 조회하면 401(C004)로 응답한다")
    void anonymousMySpotsReturnsUnauthorizedApiResponse() throws Exception {
        mockMvc.perform(get("/v1/users/me/my-spots"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("C004"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("401 응답은 JSON 컨텐츠 타입으로 내려간다")
    void unauthorizedResponseIsJson() throws Exception {
        mockMvc.perform(post("/v1/spots/1/likes"))
            .andExpect(status().isUnauthorized())
            .andExpect(result -> {
                String contentType = result.getResponse().getContentType();
                if (contentType == null || !contentType.startsWith(MediaType.APPLICATION_JSON_VALUE)) {
                    throw new AssertionError("401 응답이 JSON이 아닙니다: " + contentType);
                }
            });
    }

    @Test
    @DisplayName("일반 사용자가 어드민 API를 호출하면 ApiResponse 포맷의 403으로 응답한다")
    @WithMockUser(username = "2000", roles = "USER_CUSTOMER")
    void customerCannotAccessAdminApi() throws Exception {
        mockMvc.perform(get("/v1/admin/spots"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false));
    }
}
