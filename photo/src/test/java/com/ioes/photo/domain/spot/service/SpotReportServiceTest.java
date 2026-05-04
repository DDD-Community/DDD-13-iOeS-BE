package com.ioes.photo.domain.spot.service;

import com.ioes.photo.domain.spot.dto.SpotReportRequest;
import com.ioes.photo.domain.spot.dto.SpotReportResponse;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotReport;
import com.ioes.photo.domain.spot.enums.SpotReportStatus;
import com.ioes.photo.domain.spot.enums.SpotReportType;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.repository.SpotReportRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * {@link SpotReportService} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpotReportService 단위 테스트")
class SpotReportServiceTest {

    @Mock SpotRepository       spotRepository;
    @Mock SpotReportRepository spotReportRepository;

    @InjectMocks SpotReportService spotReportService;

    private static final Long   USER_ID  = 42L;
    private static final Long   SPOT_ID  = 7L;
    private static final Long   REPORT_ID = 100L;

    // ── report ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("report()")
    class Report {

        @Test
        @DisplayName("PUBLISHED 스팟에 신고하면 저장된 리포트 id를 반환한다")
        void returnsReportId_whenSpotIsPublished() {
            SpotReportRequest request = new SpotReportRequest(SpotReportType.LOCATION_ERROR, "위치가 잘못됐어요");
            Spot publishedSpot = buildSpot(SPOT_ID, SpotStatus.PUBLISHED);
            SpotReport saved = buildReport(REPORT_ID);

            given(spotRepository.findById(SPOT_ID)).willReturn(Optional.of(publishedSpot));
            given(spotReportRepository.save(any(SpotReport.class))).willReturn(saved);

            SpotReportResponse response = spotReportService.report(USER_ID, SPOT_ID, request);

            assertThat(response.reportId()).isEqualTo(REPORT_ID);
        }

        @Test
        @DisplayName("존재하지 않는 스팟이면 BusinessException을 던진다")
        void throwsBusinessException_whenSpotNotFound() {
            SpotReportRequest request = new SpotReportRequest(SpotReportType.ETC, "내용");

            given(spotRepository.findById(SPOT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> spotReportService.report(USER_ID, SPOT_ID, request))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("PENDING 상태의 스팟이면 BusinessException을 던진다")
        void throwsBusinessException_whenSpotIsPending() {
            SpotReportRequest request = new SpotReportRequest(SpotReportType.WRONG_NAME, "내용");
            Spot pendingSpot = buildSpot(SPOT_ID, SpotStatus.PENDING);

            given(spotRepository.findById(SPOT_ID)).willReturn(Optional.of(pendingSpot));

            assertThatThrownBy(() -> spotReportService.report(USER_ID, SPOT_ID, request))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("REJECTED 상태의 스팟이면 BusinessException을 던진다")
        void throwsBusinessException_whenSpotIsRejected() {
            SpotReportRequest request = new SpotReportRequest(SpotReportType.ETC, "내용");
            Spot rejectedSpot = buildSpot(SPOT_ID, SpotStatus.REJECTED);

            given(spotRepository.findById(SPOT_ID)).willReturn(Optional.of(rejectedSpot));

            assertThatThrownBy(() -> spotReportService.report(USER_ID, SPOT_ID, request))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("신고 시 status는 PENDING으로 초기화된다")
        void setsStatusToPending_onCreate() {
            SpotReportRequest request = new SpotReportRequest(SpotReportType.LOCATION_ERROR, "내용");
            Spot publishedSpot = buildSpot(SPOT_ID, SpotStatus.PUBLISHED);
            SpotReport saved = buildReport(REPORT_ID);

            given(spotRepository.findById(SPOT_ID)).willReturn(Optional.of(publishedSpot));
            given(spotReportRepository.save(any(SpotReport.class))).willReturn(saved);

            spotReportService.report(USER_ID, SPOT_ID, request);

            ArgumentCaptor<SpotReport> captor = ArgumentCaptor.forClass(SpotReport.class);
            then(spotReportRepository).should().save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(SpotReportStatus.PENDING);
        }

        @Test
        @DisplayName("신고 엔티티에 userId, spotId, type, content가 올바르게 설정된다")
        void setsAllFieldsCorrectly() {
            SpotReportRequest request = new SpotReportRequest(SpotReportType.WRONG_NAME, "이름이 틀려요");
            Spot publishedSpot = buildSpot(SPOT_ID, SpotStatus.PUBLISHED);
            SpotReport saved = buildReport(REPORT_ID);

            given(spotRepository.findById(SPOT_ID)).willReturn(Optional.of(publishedSpot));
            given(spotReportRepository.save(any(SpotReport.class))).willReturn(saved);

            spotReportService.report(USER_ID, SPOT_ID, request);

            ArgumentCaptor<SpotReport> captor = ArgumentCaptor.forClass(SpotReport.class);
            then(spotReportRepository).should().save(captor.capture());

            SpotReport captured = captor.getValue();
            assertThat(captured.getUserId()).isEqualTo(USER_ID);
            assertThat(captured.getSpotId()).isEqualTo(SPOT_ID);
            assertThat(captured.getType()).isEqualTo(SpotReportType.WRONG_NAME);
            assertThat(captured.getContent()).isEqualTo("이름이 틀려요");
        }

        @Test
        @DisplayName("신고 유형 ETC도 정상 저장된다")
        void savesEtcReportType() {
            SpotReportRequest request = new SpotReportRequest(SpotReportType.ETC, "기타 사유");
            Spot publishedSpot = buildSpot(SPOT_ID, SpotStatus.PUBLISHED);
            SpotReport saved = buildReport(REPORT_ID);

            given(spotRepository.findById(SPOT_ID)).willReturn(Optional.of(publishedSpot));
            given(spotReportRepository.save(any(SpotReport.class))).willReturn(saved);

            spotReportService.report(USER_ID, SPOT_ID, request);

            ArgumentCaptor<SpotReport> captor = ArgumentCaptor.forClass(SpotReport.class);
            then(spotReportRepository).should().save(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(SpotReportType.ETC);
        }
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private static Spot buildSpot(Long id, SpotStatus status) {
        Spot spot = Spot.builder()
            .name("테스트스팟")
            .theme(SpotTheme.SUNSET)
            .latitude(37.5)
            .longitude(127.0)
            .status(status)
            .build();
        ReflectionTestUtils.setField(spot, "id", id);
        return spot;
    }

    private static SpotReport buildReport(Long id) {
        SpotReport report = SpotReport.builder()
            .spotId(SPOT_ID)
            .userId(USER_ID)
            .type(SpotReportType.LOCATION_ERROR)
            .content("내용")
            .build();
        ReflectionTestUtils.setField(report, "id", id);
        return report;
    }
}
