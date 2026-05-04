package com.ioes.photo.domain.spot.entity;

import com.ioes.photo.domain.spot.enums.SpotReportStatus;
import com.ioes.photo.domain.spot.enums.SpotReportType;
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
    @DisplayName("builder로 생성한 SpotReport는 전달한 userId, spotId, type, content를 가진다")
    void builderSetsAllFields() {
        SpotReport report = SpotReport.builder()
            .spotId(7L)
            .userId(42L)
            .type(SpotReportType.LOCATION_ERROR)
            .content("위치가 잘못됐어요")
            .build();

        assertThat(report.getSpotId()).isEqualTo(7L);
        assertThat(report.getUserId()).isEqualTo(42L);
        assertThat(report.getType()).isEqualTo(SpotReportType.LOCATION_ERROR);
        assertThat(report.getContent()).isEqualTo("위치가 잘못됐어요");
    }

    @Test
    @DisplayName("생성 시 status는 항상 PENDING으로 초기화된다")
    void statusIsAlwaysPendingOnCreation() {
        SpotReport report = SpotReport.builder()
            .spotId(1L)
            .userId(1L)
            .type(SpotReportType.ETC)
            .content("기타 사유")
            .build();

        assertThat(report.getStatus()).isEqualTo(SpotReportStatus.PENDING);
    }

    @Test
    @DisplayName("신고 유형 WRONG_NAME도 정상 저장된다")
    void acceptsWrongNameType() {
        SpotReport report = SpotReport.builder()
            .spotId(2L)
            .userId(5L)
            .type(SpotReportType.WRONG_NAME)
            .content("이름이 틀려요")
            .build();

        assertThat(report.getType()).isEqualTo(SpotReportType.WRONG_NAME);
    }

    @Test
    @DisplayName("신고 유형 ETC도 정상 저장된다")
    void acceptsEtcType() {
        SpotReport report = SpotReport.builder()
            .spotId(3L)
            .userId(9L)
            .type(SpotReportType.ETC)
            .content("기타")
            .build();

        assertThat(report.getType()).isEqualTo(SpotReportType.ETC);
    }
}
