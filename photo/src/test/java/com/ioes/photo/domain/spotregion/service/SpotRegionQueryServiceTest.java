package com.ioes.photo.domain.spotregion.service;

import com.ioes.photo.domain.spotregion.dto.RegionListResponse;
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
 * {@link SpotRegionQueryService} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpotRegionQueryService 단위 테스트")
class SpotRegionQueryServiceTest {

    @Mock SpotRegionRepository spotRegionRepository;

    @InjectMocks SpotRegionQueryService spotRegionQueryService;

    @Test
    @DisplayName("활성화된 지역을 region_id, region_name만 담아 반환한다")
    void returnsActiveRegions() {
        given(spotRegionRepository.findAllByActiveTrueOrderByRegionIdAsc())
            .willReturn(List.of(region(1L, "서울"), region(2L, "대전")));

        RegionListResponse response = spotRegionQueryService.findActiveRegions();

        assertThat(response.regions()).hasSize(2);
        assertThat(response.regions().get(0).regionId()).isEqualTo(1L);
        assertThat(response.regions().get(0).regionName()).isEqualTo("서울");
        assertThat(response.regions().get(1).regionId()).isEqualTo(2L);
        assertThat(response.regions().get(1).regionName()).isEqualTo("대전");
    }

    @Test
    @DisplayName("활성화된 지역이 없으면 빈 목록을 반환한다")
    void returnsEmpty_whenNoActiveRegions() {
        given(spotRegionRepository.findAllByActiveTrueOrderByRegionIdAsc()).willReturn(List.of());

        RegionListResponse response = spotRegionQueryService.findActiveRegions();

        assertThat(response.regions()).isEmpty();
    }

    private SpotRegion region(Long regionId, String regionName) {
        SpotRegion region = BeanUtils.instantiateClass(SpotRegion.class);
        ReflectionTestUtils.setField(region, "regionId", regionId);
        ReflectionTestUtils.setField(region, "regionName", regionName);
        ReflectionTestUtils.setField(region, "active", true);
        return region;
    }
}
