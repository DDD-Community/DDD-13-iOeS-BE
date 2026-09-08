package com.ioes.photo.domain.spot.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * {@link Spot} 상태 전이 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("Spot 상태 전이 단위 테스트")
class SpotTest {

    private static final Long REVIEWER_ID = 99L;

    @Test
    @DisplayName("status 를 지정하지 않으면 기본값은 DRAFT(나만보기) 다")
    void defaultsToDraft_whenStatusNotGiven() {
        Spot spot = buildSpot(null);

        assertThat(spot.getStatus()).isEqualTo(SpotStatus.DRAFT);
    }

    @Nested
    @DisplayName("requestOpen()")
    class RequestOpen {

        @Test
        @DisplayName("DRAFT 는 오픈 신청 시 PENDING(검수중)으로 전이되고 appliedAt 이 세팅된다")
        void draftBecomesPending() {
            Spot spot = buildSpot(SpotStatus.DRAFT);
            LocalDateTime now = LocalDateTime.now();

            spot.requestOpen(now);

            assertThat(spot.getStatus()).isEqualTo(SpotStatus.PENDING);
            assertThat(spot.getAppliedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("REJECTED 는 재신청 시 RE_REVIEW_PENDING(재검토대기)으로 전이된다")
        void rejectedBecomesReReviewPending() {
            Spot spot = buildSpot(SpotStatus.REJECTED);
            LocalDateTime now = LocalDateTime.now();

            spot.requestOpen(now);

            assertThat(spot.getStatus()).isEqualTo(SpotStatus.RE_REVIEW_PENDING);
            assertThat(spot.getAppliedAt()).isEqualTo(now);
        }
    }

    @Nested
    @DisplayName("applyReview()")
    class ApplyReview {

        @Test
        @DisplayName("승인 시 PUBLISHED 로 전이되고 처리자/처리일시가 세팅된다")
        void approveBecomesPublished() {
            Spot spot = buildSpot(SpotStatus.PENDING);
            LocalDateTime now = LocalDateTime.now();

            spot.applyReview(true, REVIEWER_ID, now);

            assertThat(spot.getStatus()).isEqualTo(SpotStatus.PUBLISHED);
            assertThat(spot.getReviewerId()).isEqualTo(REVIEWER_ID);
            assertThat(spot.getReviewedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("반려 시 REJECTED 로 전이된다")
        void rejectBecomesRejected() {
            Spot spot = buildSpot(SpotStatus.RE_REVIEW_PENDING);

            spot.applyReview(false, REVIEWER_ID, LocalDateTime.now());

            assertThat(spot.getStatus()).isEqualTo(SpotStatus.REJECTED);
        }
    }

    @Nested
    @DisplayName("상태 판별")
    class StatusChecks {

        @Test
        @DisplayName("DRAFT/REJECTED 만 오픈 신청 가능하다")
        void isOpenRequestable() {
            assertThat(buildSpot(SpotStatus.DRAFT).isOpenRequestable()).isTrue();
            assertThat(buildSpot(SpotStatus.REJECTED).isOpenRequestable()).isTrue();
            assertThat(buildSpot(SpotStatus.PENDING).isOpenRequestable()).isFalse();
            assertThat(buildSpot(SpotStatus.PUBLISHED).isOpenRequestable()).isFalse();
        }

        @Test
        @DisplayName("PENDING/RE_REVIEW_PENDING 만 검수 가능하다")
        void isReviewable() {
            assertThat(buildSpot(SpotStatus.PENDING).isReviewable()).isTrue();
            assertThat(buildSpot(SpotStatus.RE_REVIEW_PENDING).isReviewable()).isTrue();
            assertThat(buildSpot(SpotStatus.DRAFT).isReviewable()).isFalse();
            assertThat(buildSpot(SpotStatus.PUBLISHED).isReviewable()).isFalse();
            assertThat(buildSpot(SpotStatus.REJECTED).isReviewable()).isFalse();
        }
    }

    @Nested
    @DisplayName("relYn(노출) 플래그")
    class ReleaseFlag {

        @Test
        @DisplayName("PUBLISHED 로 곧바로 생성되면(어드민 큐레이션/배치 등록) relYn 기본값은 Y 다")
        void defaultsToReleased_whenCreatedAsPublished() {
            Spot spot = buildSpot(SpotStatus.PUBLISHED);

            assertThat(spot.isReleased()).isTrue();
        }

        @Test
        @DisplayName("PUBLISHED 가 아닌 상태로 생성되면 relYn 기본값은 N 이다")
        void defaultsToUnreleased_whenNotPublished() {
            assertThat(buildSpot(SpotStatus.DRAFT).isReleased()).isFalse();
            assertThat(buildSpot(SpotStatus.PENDING).isReleased()).isFalse();
        }

        @Test
        @DisplayName("검수 승인 시 relYn 이 자동으로 Y 가 된다")
        void applyReviewApproved_setsReleased() {
            Spot spot = buildSpot(SpotStatus.PENDING);

            spot.applyReview(true, REVIEWER_ID, LocalDateTime.now());

            assertThat(spot.isReleased()).isTrue();
        }

        @Test
        @DisplayName("검수 반려 시 relYn 은 그대로 N 이다")
        void applyReviewRejected_keepsUnreleased() {
            Spot spot = buildSpot(SpotStatus.PENDING);

            spot.applyReview(false, REVIEWER_ID, LocalDateTime.now());

            assertThat(spot.isReleased()).isFalse();
        }

        @Test
        @DisplayName("공개 해제(cancelPublication) 시 relYn 이 N 으로 초기화된다")
        void cancelPublication_resetsToUnreleased() {
            Spot spot = buildSpot(SpotStatus.PUBLISHED);
            assertThat(spot.isReleased()).isTrue();

            spot.cancelPublication();

            assertThat(spot.isReleased()).isFalse();
        }

        @Test
        @DisplayName("release()/unrelease() 로 relYn 을 직접 켜고 끌 수 있다")
        void releaseAndUnrelease() {
            Spot spot = buildSpot(SpotStatus.PUBLISHED);

            spot.unrelease();
            assertThat(spot.isReleased()).isFalse();

            spot.release();
            assertThat(spot.isReleased()).isTrue();
        }

        @Test
        @DisplayName("PUBLISHED 상태일 때만 노출을 껐다 켤 수 있다")
        void isReleaseControllable() {
            assertThat(buildSpot(SpotStatus.PUBLISHED).isReleaseControllable()).isTrue();
            assertThat(buildSpot(SpotStatus.DRAFT).isReleaseControllable()).isFalse();
            assertThat(buildSpot(SpotStatus.PENDING).isReleaseControllable()).isFalse();
            assertThat(buildSpot(SpotStatus.REJECTED).isReleaseControllable()).isFalse();
        }
    }

    private static Spot buildSpot(SpotStatus status) {
        Spot spot = Spot.builder()
            .name("테스트스팟")
            .theme(SpotTheme.SUNSET)
            .latitude(37.5)
            .longitude(127.0)
            .status(status)
            .userId(1L)
            .build();
        ReflectionTestUtils.setField(spot, "id", 7L);
        return spot;
    }
}
