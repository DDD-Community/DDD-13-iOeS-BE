package com.ioes.photo.domain.spot.service;

import com.ioes.photo.domain.spot.dto.SpotReportRequest;
import com.ioes.photo.domain.spot.dto.SpotReportResponse;
import com.ioes.photo.domain.spot.entity.SpotReport;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.error.SpotErrorCode;
import com.ioes.photo.domain.spot.repository.SpotReportRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 스팟 신고 서비스.
 *
 * @author 황제연
 */
@Service
@RequiredArgsConstructor
public class SpotReportService {

    private final SpotRepository spotRepository;
    private final SpotReportRepository spotReportRepository;

    @Transactional
    public SpotReportResponse report(Long userId, Long spotId, SpotReportRequest request) {
        spotRepository.findById(spotId)
            .filter(spot -> spot.getStatus() == SpotStatus.PUBLISHED)
            .orElseThrow(() -> new BusinessException(SpotErrorCode.SPOT_NOT_FOUND));

        SpotReport report = SpotReport.builder()
            .spotId(spotId)
            .userId(userId)
            .type(request.type())
            .content(request.content())
            .build();

        return new SpotReportResponse(spotReportRepository.save(report).getId());
    }
}
