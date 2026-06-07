package com.ioes.photo.domain.spot.entity;

import com.ioes.photo.domain.spot.enums.SpotReportStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SpotReport} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("SpotReport 단위 테스트")
class SpotReportTest {

    @Test
    @DisplayName("builder로 생성한 SpotReport는 전달한 userId, spotId, content를 가진다")
    void builderSetsAllFields() {
        SpotReport report = SpotReport.builder()
            .spotId(7L)
            .userId(42L)
            .content("위치가 잘못됐어요")
            .build();

        assertThat(report.getSpotId()).isEqualTo(7L);
        assertThat(report.getUserId()).isEqualTo(42L);
        assertThat(report.getContent()).isEqualTo("위치가 잘못됐어요");
    }

    @Test
    @DisplayName("생성 시 status는 항상 PENDING으로 초기화된다")
    void statusIsAlwaysPendingOnCreation() {
        SpotReport report = SpotReport.builder()
            .spotId(1L)
            .userId(1L)
            .content("기타 사유")
            .build();

        assertThat(report.getStatus()).isEqualTo(SpotReportStatus.PENDING);
    }
}
