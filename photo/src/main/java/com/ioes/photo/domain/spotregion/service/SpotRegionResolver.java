package com.ioes.photo.domain.spotregion.service;

import com.ioes.photo.domain.spotregion.entity.SpotRegion;
import com.ioes.photo.domain.spotregion.repository.SpotRegionRepository;
import com.ioes.photo.global.common.util.NullUtils;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 스팟 주소로 지역(region_id)을 판별한다.
 *
 * region_name 을 주소 접두어로 그대로 사용한다(예: region_name="서울" -> address "서울특별시 ..." 매칭).
 * 활성 지역만 대상으로 하며, 여러 지역명이 동시에 접두어로 걸릴 가능성에 대비해 더 긴(구체적인) 이름을 우선한다.
 * 마이그레이션 V20 의 백필 로직(LIKE '서울%' -> 1, LIKE '대전%' -> 2)과 동일한 규칙이다.
 *
 * @author 황제연
 */
@Component
@RequiredArgsConstructor
public class SpotRegionResolver {

    private final SpotRegionRepository spotRegionRepository;

    public Long resolve(String address) {
        if (NullUtils.isBlank(address)) {
            return null;
        }

        return spotRegionRepository.findAllByActiveTrueOrderByRegionIdAsc().stream()
            .sorted(Comparator.comparingInt((SpotRegion region) -> region.getRegionName().length()).reversed())
            .filter(region -> address.startsWith(region.getRegionName()))
            .map(SpotRegion::getRegionId)
            .findFirst()
            .orElse(null);
    }
}
