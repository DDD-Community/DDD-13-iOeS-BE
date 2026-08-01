package com.ioes.photo.domain.spot.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ioes.photo.domain.spot.dto.AdminSpotDetailResponse;
import com.ioes.photo.domain.spot.dto.AdminSpotListResponse;
import com.ioes.photo.domain.spot.dto.SpotReviewRequest;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.enums.RejectionReason;
import com.ioes.photo.domain.spot.enums.ReviewDecision;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spot.repository.SpotReviewRepository;
import com.ioes.photo.domain.user.entity.User;
import com.ioes.photo.domain.user.enums.UserRole;
import com.ioes.photo.domain.user.repository.UserRepository;
import com.ioes.photo.global.auth.oauth.OAuthProvider;
import com.ioes.photo.global.storage.S3StorageService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 스팟 검수 어드민 통합 테스트 — 실제 JPA + MyBatis(H2/PostgreSQL 모드)로 목록/상세/검수 검증.
 *
 * @author 황제연
 */
@SpringBootTest
@DisplayName("스팟 검수 어드민 통합 테스트")
class SpotReviewAdminIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () ->
            "c2VjcmV0LWtleS1mb3ItdGVzdGluZy1wdXJwb3Nlcy1vbmx5LW11c3QtYmUtYXQtbGVhc3QtNjQtYnl0ZXMtbG9uZw==");
    }

    @MockitoBean RedisConnectionFactory redisConnectionFactory;
    @MockitoBean ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;
    @MockitoBean S3StorageService storageService;

    @Autowired SpotReviewQueryService spotReviewQueryService;
    @Autowired SpotReviewService spotReviewService;
    @Autowired SpotRepository spotRepository;
    @Autowired SpotReviewRepository spotReviewRepository;
    @Autowired SpotImageRepository spotImageRepository;
    @Autowired UserRepository userRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private static final Long REVIEWER_ID = 1000L;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM spot_reviews");
        jdbcTemplate.execute("DELETE FROM spot_images");
        jdbcTemplate.execute("DELETE FROM spots");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    @DisplayName("목록 정렬: 재검토대기 → 검수중(오래된순) → 완료건(처리일시 최신순), DRAFT 제외")
    void listSortingRule() {
        Long owner = saveUser("등록유저");
        Long pendingOld = saveSpot("A검수오래", SpotStatus.PENDING, owner, daysAgo(2), null);
        Long pendingNew = saveSpot("B검수최근", SpotStatus.PENDING, owner, daysAgo(1), null);
        Long reReview = saveSpot("C재검토", SpotStatus.RE_REVIEW_PENDING, owner, daysAgo(3), null);
        Long publishedOld = saveSpot("D승인오래", SpotStatus.PUBLISHED, owner, daysAgo(3), daysAgo(10));
        Long rejectedNew = saveSpot("E반려최근", SpotStatus.REJECTED, owner, daysAgo(3), daysAgo(5));
        saveSpot("F나만보기", SpotStatus.DRAFT, owner, null, null);

        AdminSpotListResponse response = spotReviewQueryService.findReviewSpots(null, null, 0, 20);

        assertThat(response.items()).extracting(AdminSpotListResponse.AdminSpotItem::id)
            .containsExactly(reReview, pendingOld, pendingNew, rejectedNew, publishedOld);
    }

    @Test
    @DisplayName("상태 필터: status=PENDING 이면 검수중 건만 반환한다")
    void listStatusFilter() {
        Long owner = saveUser("등록유저");
        saveSpot("검수중", SpotStatus.PENDING, owner, daysAgo(1), null);
        saveSpot("승인됨", SpotStatus.PUBLISHED, owner, daysAgo(1), daysAgo(1));

        AdminSpotListResponse response =
            spotReviewQueryService.findReviewSpots(SpotStatus.PENDING, null, 0, 20);

        assertThat(response.items()).extracting(AdminSpotListResponse.AdminSpotItem::status)
            .containsOnly("PENDING");
    }

    @Test
    @DisplayName("검색: 스팟명 또는 등록자 닉네임 부분일치로 검색된다")
    void listSearchByNameOrNickname() {
        Long hanRiverOwner = saveUser("한강러버");
        Long otherOwner = saveUser("산책러");
        saveSpot("노을 명소", SpotStatus.PENDING, hanRiverOwner, daysAgo(1), null);
        saveSpot("한강 계단", SpotStatus.PENDING, otherOwner, daysAgo(1), null);
        saveSpot("무관 스팟", SpotStatus.PENDING, otherOwner, daysAgo(1), null);

        AdminSpotListResponse response = spotReviewQueryService.findReviewSpots(null, "한강", 0, 20);

        assertThat(response.items()).hasSize(2)
            .extracting(AdminSpotListResponse.AdminSpotItem::name)
            .containsExactlyInAnyOrder("노을 명소", "한강 계단");
    }

    @Test
    @DisplayName("페이지네이션: size 초과 시 hasNext=true")
    void listPagination() {
        Long owner = saveUser("등록유저");
        saveSpot("s1", SpotStatus.PENDING, owner, daysAgo(1), null);
        saveSpot("s2", SpotStatus.PENDING, owner, daysAgo(2), null);
        saveSpot("s3", SpotStatus.PENDING, owner, daysAgo(3), null);

        AdminSpotListResponse response = spotReviewQueryService.findReviewSpots(null, null, 0, 2);

        assertThat(response.items()).hasSize(2);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    @DisplayName("승인: PENDING → PUBLISHED 전이 + 승인 이력 저장")
    void approveFlow() {
        Long owner = saveUser("등록유저");
        Long spotId = saveSpot("검수대상", SpotStatus.PENDING, owner, daysAgo(1), null);

        spotReviewService.review(spotId, new SpotReviewRequest(ReviewDecision.APPROVED, null, null), REVIEWER_ID);

        Spot updated = spotRepository.findById(spotId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(SpotStatus.PUBLISHED);
        assertThat(spotReviewRepository.findBySpotIdAndDecisionOrderByCreatedAtDesc(spotId, ReviewDecision.REJECTED))
            .isEmpty();
    }

    @Test
    @DisplayName("반려 후 상세: 반려 이력이 최신순으로 조회된다")
    void rejectThenDetailShowsHistory() {
        Long owner = saveUser("등록유저");
        Long spotId = saveSpot("검수대상", SpotStatus.PENDING, owner, daysAgo(1), null);
        spotImageRepository.save(SpotImage.create(spotId, "prod/private/spots/1/original/202607/a.jpg"));

        spotReviewService.review(spotId,
            new SpotReviewRequest(ReviewDecision.REJECTED, RejectionReason.LOW_QUALITY, null), REVIEWER_ID);

        AdminSpotDetailResponse detail = spotReviewQueryService.getReviewSpotDetail(spotId);

        assertThat(detail.status()).isEqualTo(SpotStatus.REJECTED.name());
        assertThat(detail.rejectionHistory()).hasSize(1);
        assertThat(detail.rejectionHistory().get(0).reason()).isEqualTo("LOW_QUALITY");
        assertThat(detail.rejectionHistory().get(0).reasonLabel()).isEqualTo("사진 상태 불량");
        assertThat(detail.userTrust().totalRejected()).isEqualTo(1L);
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private LocalDateTime daysAgo(int days) {
        return LocalDateTime.now().minusDays(days);
    }

    private Long saveUser(String nickname) {
        User user = User.builder()
            .provider(OAuthProvider.KAKAO)
            .providerUserId("pid-" + nickname)
            .nickname(nickname)
            .role(UserRole.USER_CUSTOMER)
            .build();
        return userRepository.save(user).getId();
    }

    private Long saveSpot(String name, SpotStatus status, Long userId,
                          LocalDateTime appliedAt, LocalDateTime reviewedAt) {
        Spot spot = Spot.builder()
            .name(name)
            .theme(SpotTheme.SUNSET)
            .latitude(37.5)
            .longitude(127.0)
            .status(status)
            .userId(userId)
            .build();
        Spot saved = spotRepository.save(spot);
        jdbcTemplate.update("UPDATE spots SET applied_at = ?, reviewed_at = ? WHERE id = ?",
            appliedAt, reviewedAt, saved.getId());
        return saved.getId();
    }
}
