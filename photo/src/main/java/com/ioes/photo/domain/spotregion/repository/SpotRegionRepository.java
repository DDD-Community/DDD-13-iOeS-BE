package com.ioes.photo.domain.spotregion.repository;

import com.ioes.photo.domain.spotregion.entity.SpotRegion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 지역 참조 데이터 리포지토리.
 *
 * @author 황제연
 */
public interface SpotRegionRepository extends JpaRepository<SpotRegion, Long> {

    List<SpotRegion> findAllByActiveTrueOrderByRegionIdAsc();
}
