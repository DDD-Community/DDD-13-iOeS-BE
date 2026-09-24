package com.ioes.photo.domain.spotregion.service;

import com.ioes.photo.domain.spotregion.dto.RegionListResponse;
import com.ioes.photo.domain.spotregion.repository.SpotRegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 지역 조회 서비스.
 *
 * @author 황제연
 */
@Service
@RequiredArgsConstructor
public class SpotRegionQueryService {

    private final SpotRegionRepository spotRegionRepository;

    public RegionListResponse findActiveRegions() {
        return RegionListResponse.from(spotRegionRepository.findAllByActiveTrueOrderByRegionIdAsc());
    }
}
