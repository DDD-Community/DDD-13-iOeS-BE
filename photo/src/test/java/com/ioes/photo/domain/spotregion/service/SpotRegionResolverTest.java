package com.ioes.photo.domain.spotregion.service;

import com.ioes.photo.domain.spotregion.entity.SpotRegion;
import com.ioes.photo.domain.spotregion.repository.SpotRegionRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * {@link SpotRegionResolver} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpotRegionResolver 단위 테스트")
class SpotRegionResolverTest {

    @Mock SpotRegionRepository spotRegionRepository;

    @InjectMocks SpotRegionResolver resolver;

    @Test
    @DisplayName("address가 지역명으로 시작하면 해당 region_id를 반환한다")
    void resolvesRegionId_whenAddressStartsWithRegionName() {
        given(spotRegionRepository.findAllByActiveTrueOrderByRegionIdAsc())
            .willReturn(List.of(region(1L, "서울"), region(2L, "대전")));

        Long regionId = resolver.resolve("서울특별시 마포구 월드컵로 21");

        assertThat(regionId).isEqualTo(1L);
    }

    @Test
    @DisplayName("어떤 지역명과도 매칭되지 않으면 null을 반환한다")
    void returnsNull_whenNoRegionMatches() {
        given(spotRegionRepository.findAllByActiveTrueOrderByRegionIdAsc())
            .willReturn(List.of(region(1L, "서울"), region(2L, "대전")));

        Long regionId = resolver.resolve("부산광역시 해운대구");

        assertThat(regionId).isNull();
    }

    @Test
    @DisplayName("address가 null이면 조회 없이 null을 반환한다")
    void returnsNull_whenAddressNull() {
        Long regionId = resolver.resolve(null);

        assertThat(regionId).isNull();
    }

    @Test
    @DisplayName("여러 지역명이 동시에 접두어로 걸리면 더 긴(구체적인) 이름을 우선한다")
    void prefersLongerRegionName_whenMultiplePrefixesMatch() {
        given(spotRegionRepository.findAllByActiveTrueOrderByRegionIdAsc())
            .willReturn(List.of(region(1L, "서울"), region(3L, "서울특별시")));

        Long regionId = resolver.resolve("서울특별시 마포구");

        assertThat(regionId).isEqualTo(3L);
    }

    private SpotRegion region(Long regionId, String regionName) {
        SpotRegion region = BeanUtils.instantiateClass(SpotRegion.class);
        ReflectionTestUtils.setField(region, "regionId", regionId);
        ReflectionTestUtils.setField(region, "regionName", regionName);
        ReflectionTestUtils.setField(region, "active", true);
        return region;
    }
}
