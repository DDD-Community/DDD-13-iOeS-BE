package com.ioes.photo.domain.spot.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ioes.photo.domain.spot.dto.AdminSpotListResponse;
import com.ioes.photo.domain.spot.dto.SpotListResponse;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.service.SpotQueryService;
import com.ioes.photo.domain.spot.service.SpotReviewQueryService;
import com.ioes.photo.global.storage.S3StorageService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * CodedEnum({@code @RequestParam}) 전역 컨버터의 실제 바인딩 동작 통합 테스트.
 *
 * code 값과 enum 이름 어느 쪽으로 요청해도 바인딩되며, 알 수 없는 값은 400(C002)으로 응답하는지 확인한다.
 *
 * @author 황제연
 */
@SpringBootTest
@DisplayName("CodedEnum 파라미터 바인딩 통합 테스트")
class SpotThemeParamBindingIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () ->
            "c2VjcmV0LWtleS1mb3ItdGVzdGluZy1wdXJwb3Nlcy1vbmx5LW11c3QtYmUtYXQtbGVhc3QtNjQtYnl0ZXMtbG9uZw==");
    }

    private static final String ADMIN_ID = "1000";
    private static final String CUSTOMER_ID = "2000";
    private static final String LIST_URI = "/v1/spots";
    private static final String ADMIN_LIST_URI = "/v1/admin/spots";

    @MockitoBean RedisConnectionFactory redisConnectionFactory;
    @MockitoBean ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;
    @MockitoBean S3StorageService s3StorageService;

    @MockitoBean SpotQueryService spotQueryService;
    @MockitoBean SpotReviewQueryService spotReviewQueryService;

    @Autowired WebApplicationContext webApplicationContext;

    MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
    }

    // 운영 설정(security.public-get-urls)에는 /v1/spots 가 비로그인 허용이지만,
    // 테스트 프로필 설정에는 해당 목록이 없어 이 테스트에서는 로그인 상태로 검증한다.
    // (이 테스트의 목적은 인증 여부가 아니라 theme 파라미터의 CodedEnum 바인딩이다.)
    @Nested
    @DisplayName("theme 파라미터 (공개 API, SpotController)")
    class ThemeParam {

        @Test
        @WithMockUser(username = CUSTOMER_ID, roles = "USER_CUSTOMER")
        @DisplayName("code(SS)로 조회하면 SpotTheme.SUNSET으로 바인딩된다")
        void bindsByCode() throws Exception {
            given(spotQueryService.findSpots(anyInt(), any(), any(), any(), any(), any(), any()))
                .willReturn(new SpotListResponse(List.of(), 0, false));

            mockMvc.perform(get(LIST_URI).param("regionId", "1").param("theme", "SS"))
                .andExpect(status().isOk());

            then(spotQueryService).should()
                .findSpots(eq(0), eq(List.of(1L)), eq(List.of(SpotTheme.SUNSET)), any(), any(), any(), any());
        }

        @Test
        @WithMockUser(username = CUSTOMER_ID, roles = "USER_CUSTOMER")
        @DisplayName("enum 이름(SUNSET)으로도 조회할 수 있다")
        void bindsByName() throws Exception {
            given(spotQueryService.findSpots(anyInt(), any(), any(), any(), any(), any(), any()))
                .willReturn(new SpotListResponse(List.of(), 0, false));

            mockMvc.perform(get(LIST_URI).param("regionId", "1").param("theme", "SUNSET"))
                .andExpect(status().isOk());

            then(spotQueryService).should()
                .findSpots(eq(0), eq(List.of(1L)), eq(List.of(SpotTheme.SUNSET)), any(), any(), any(), any());
        }

        @Test
        @WithMockUser(username = CUSTOMER_ID, roles = "USER_CUSTOMER")
        @DisplayName("다중 선택 시 ?theme=SS&theme=YS 형식으로 반복 전달된 값이 모두 바인딩된다")
        void bindsMultipleValues() throws Exception {
            given(spotQueryService.findSpots(anyInt(), any(), any(), any(), any(), any(), any()))
                .willReturn(new SpotListResponse(List.of(), 0, false));

            mockMvc.perform(get(LIST_URI).param("regionId", "1").param("theme", "SS", "YS"))
                .andExpect(status().isOk());

            then(spotQueryService).should()
                .findSpots(eq(0), eq(List.of(1L)), eq(List.of(SpotTheme.SUNSET, SpotTheme.YUNSEUL)), any(), any(), any(), any());
        }

        @Test
        @WithMockUser(username = CUSTOMER_ID, roles = "USER_CUSTOMER")
        @DisplayName("code도 이름도 아닌 값이면 400(C002)이다")
        void rejectsUnknownValue() throws Exception {
            mockMvc.perform(get(LIST_URI).param("regionId", "1").param("theme", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C002"));
        }
    }

    @Nested
    @DisplayName("status 파라미터 (어드민 API, SpotReviewAdminController)")
    class StatusParam {

        @Test
        @WithMockUser(username = ADMIN_ID, roles = "USER_ADMIN")
        @DisplayName("code(B)로 조회하면 SpotStatus.PUBLISHED로 바인딩된다")
        void bindsByCode() throws Exception {
            given(spotReviewQueryService.findReviewSpots(any(), any(), anyInt(), anyInt()))
                .willReturn(new AdminSpotListResponse(List.of(), 0, false));

            mockMvc.perform(get(ADMIN_LIST_URI).param("status", "B"))
                .andExpect(status().isOk());

            then(spotReviewQueryService).should()
                .findReviewSpots(eq(SpotStatus.PUBLISHED), any(), anyInt(), anyInt());
        }

        @Test
        @WithMockUser(username = ADMIN_ID, roles = "USER_ADMIN")
        @DisplayName("enum 이름(PUBLISHED)으로도 조회할 수 있다")
        void bindsByName() throws Exception {
            given(spotReviewQueryService.findReviewSpots(any(), any(), anyInt(), anyInt()))
                .willReturn(new AdminSpotListResponse(List.of(), 0, false));

            mockMvc.perform(get(ADMIN_LIST_URI).param("status", "PUBLISHED"))
                .andExpect(status().isOk());

            then(spotReviewQueryService).should()
                .findReviewSpots(eq(SpotStatus.PUBLISHED), any(), anyInt(), anyInt());
        }
    }
}
