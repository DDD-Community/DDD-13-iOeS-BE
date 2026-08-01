package com.ioes.photo.domain.spot.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ioes.photo.domain.spot.dto.AdminSpotDetailResponse;
import com.ioes.photo.domain.spot.dto.AdminSpotDetailResponse.UserTrust;
import com.ioes.photo.domain.spot.dto.AdminSpotListResponse;
import com.ioes.photo.domain.spot.dto.SpotReviewResultResponse;
import com.ioes.photo.domain.spot.service.SpotReviewQueryService;
import com.ioes.photo.domain.spot.service.SpotReviewService;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 스팟 검수 어드민 컨트롤러 웹 계층 테스트 — {@code @AdminOnly} 권한 가드 검증.
 *
 * 권한이 없는 요청이 403/401 로 끊기고 서비스 계층까지 도달하지 않는 것을 함께 확인한다.
 *
 * @author 황제연
 */
@SpringBootTest
@DisplayName("스팟 검수 어드민 컨트롤러 권한 테스트")
class SpotReviewAdminControllerTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () ->
            "c2VjcmV0LWtleS1mb3ItdGVzdGluZy1wdXJwb3Nlcy1vbmx5LW11c3QtYmUtYXQtbGVhc3QtNjQtYnl0ZXMtbG9uZw==");
    }

    private static final String ADMIN_ID = "1000";
    private static final String CUSTOMER_ID = "2000";
    private static final String LIST_URI = "/v1/admin/spots";
    private static final String DETAIL_URI = "/v1/admin/spots/1";
    private static final String REVIEW_URI = "/v1/admin/spots/1/reviews";
    private static final String APPROVE_BODY = "{\"decision\":\"APPROVED\"}";

    @MockitoBean RedisConnectionFactory redisConnectionFactory;
    @MockitoBean ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;
    @MockitoBean S3StorageService s3StorageService;

    @MockitoBean SpotReviewQueryService spotReviewQueryService;
    @MockitoBean SpotReviewService spotReviewService;

    @Autowired WebApplicationContext webApplicationContext;

    MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
    }

    @Nested
    @DisplayName("USER_ADMIN 권한")
    class AsAdmin {

        @Test
        @WithMockUser(username = ADMIN_ID, roles = "USER_ADMIN")
        @DisplayName("목록 조회에 접근할 수 있다")
        void allowsList() throws Exception {
            given(spotReviewQueryService.findReviewSpots(any(), any(), anyInt(), anyInt()))
                .willReturn(new AdminSpotListResponse(List.of(), 0, false));

            mockMvc.perform(get(LIST_URI))
                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = ADMIN_ID, roles = "USER_ADMIN")
        @DisplayName("상세 조회에 접근할 수 있다")
        void allowsDetail() throws Exception {
            given(spotReviewQueryService.getReviewSpotDetail(anyLong())).willReturn(detailResponse());

            mockMvc.perform(get(DETAIL_URI))
                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = ADMIN_ID, roles = "USER_ADMIN")
        @DisplayName("검수 요청 시 인증 주체의 ID가 검수자로 전달된다")
        void passesAuthenticatedUserAsReviewer() throws Exception {
            given(spotReviewService.review(anyLong(), any(), anyLong()))
                .willReturn(new SpotReviewResultResponse(1L, "PUBLISHED"));

            mockMvc.perform(post(REVIEW_URI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(APPROVE_BODY))
                .andExpect(status().isOk());

            then(spotReviewService).should().review(eq(1L), any(), eq(Long.valueOf(ADMIN_ID)));
        }
    }

    @Nested
    @DisplayName("권한 없는 요청")
    class Denied {

        @Test
        @WithMockUser(username = CUSTOMER_ID, roles = "USER_CUSTOMER")
        @DisplayName("일반 사용자는 목록 조회 시 403 이며 서비스가 호출되지 않는다")
        void rejectsCustomerOnList() throws Exception {
            mockMvc.perform(get(LIST_URI))
                .andExpect(status().isForbidden());

            then(spotReviewQueryService).should(never())
                .findReviewSpots(any(), any(), anyInt(), anyInt());
        }

        @Test
        @WithMockUser(username = CUSTOMER_ID, roles = "USER_CUSTOMER")
        @DisplayName("일반 사용자는 검수 요청 시 403 이며 검수가 수행되지 않는다")
        void rejectsCustomerOnReview() throws Exception {
            mockMvc.perform(post(REVIEW_URI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(APPROVE_BODY))
                .andExpect(status().isForbidden());

            then(spotReviewService).should(never()).review(anyLong(), any(), anyLong());
        }

        @Test
        @DisplayName("미인증 요청은 401 이다")
        void rejectsAnonymous() throws Exception {
            mockMvc.perform(get(LIST_URI))
                .andExpect(status().isUnauthorized());
        }
    }

    private static AdminSpotDetailResponse detailResponse() {
        return new AdminSpotDetailResponse(1L, "스팟", "유저", "PENDING", null, List.of(),
            "주소", 37.5, 127.0, "코멘트", null, "SUNSET", "노을", List.of(),
            new UserTrust(null, 0, 0, 0));
    }
}
