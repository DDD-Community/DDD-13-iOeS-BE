package com.ioes.photo.domain.crowdarea.service;

import com.ioes.photo.domain.crowdarea.config.CrowdMappingProperties;
import com.ioes.photo.domain.crowdarea.entity.CrowdArea;
import com.ioes.photo.domain.crowdarea.repository.CrowdAreaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * {@link CrowdAreaMapper} 단위 테스트.
 *
 * @author 김성민
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CrowdAreaMapper 단위 테스트")
class CrowdAreaMapperTest {

    @Mock CrowdAreaRepository crowdAreaRepository;

    private CrowdAreaMapper mapper(double maxDistanceMeters, CrowdArea... areas) {
        given(crowdAreaRepository.findAll()).willReturn(List.of(areas));
        return new CrowdAreaMapper(crowdAreaRepository, new CrowdMappingProperties(maxDistanceMeters));
    }

    @Test
    @DisplayName("여러 장소 중 좌표에서 가장 가까운 장소명을 반환한다")
    void shouldReturnNearestAreaName() {
        CrowdArea gwanghwamun = area("광화문·덕수궁", 37.5709, 126.9772);
        CrowdArea gangnam = area("강남 MICE 관광특구", 37.5110, 127.0601);
        CrowdAreaMapper mapper = mapper(3000, gwanghwamun, gangnam);

        Optional<String> result = mapper.findNearestAreaName(37.5705, 126.9775);

        assertThat(result).contains("광화문·덕수궁");
    }

    @Test
    @DisplayName("임계값 이내면 매핑한다")
    void shouldMapWithinThreshold() {
        CrowdArea area = area("광화문·덕수궁", 37.5709, 126.9772);
        CrowdAreaMapper mapper = mapper(3000, area);

        Optional<String> result = mapper.findNearestAreaName(37.5509, 126.9772);

        assertThat(result).contains("광화문·덕수궁");
    }

    @Test
    @DisplayName("임계값을 초과하면 매핑하지 않는다")
    void shouldNotMapBeyondThreshold() {
        CrowdArea area = area("광화문·덕수궁", 37.5709, 126.9772);
        CrowdAreaMapper mapper = mapper(3000, area);

        Optional<String> result = mapper.findNearestAreaName(37.6209, 126.9772);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("서울 밖 좌표는 매핑되지 않는다")
    void shouldNotMapOutsideSeoul() {
        CrowdArea area = area("광화문·덕수궁", 37.5709, 126.9772);
        CrowdAreaMapper mapper = mapper(3000, area);

        Optional<String> result = mapper.findNearestAreaName(33.4500, 126.5600);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("장소 데이터가 없으면 매핑되지 않는다")
    void shouldReturnEmptyWhenNoAreas() {
        CrowdAreaMapper mapper = mapper(3000);

        Optional<String> result = mapper.findNearestAreaName(37.5709, 126.9772);

        assertThat(result).isEmpty();
    }

    private CrowdArea area(String name, double latitude, double longitude) {
        CrowdArea area = BeanUtils.instantiateClass(CrowdArea.class);
        ReflectionTestUtils.setField(area, "areaName", name);
        ReflectionTestUtils.setField(area, "latitude", latitude);
        ReflectionTestUtils.setField(area, "longitude", longitude);
        return area;
    }
}
