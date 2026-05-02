package com.ioes.photo.domain.spot.repository;

import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 스팟 JPA 리포지토리.
 *
 * 외부 API 스케줄러의 수집 대상 조회를 위한 메서드들을 제공한다.
 *
 * @author 김성민
 */
public interface SpotRepository extends JpaRepository<Spot, Long> {

    List<Spot> findAllByStatus(SpotStatus status);

    List<Spot> findAllByStatusAndCrowdAreaNameIsNotNull(SpotStatus status);

    List<Spot> findAllByStatusAndGridNxIsNotNullAndGridNyIsNotNull(SpotStatus status);

    @Query("SELECT s FROM Spot s WHERE s.latitude BETWEEN :minLat AND :maxLat AND s.longitude BETWEEN :minLng AND :maxLng AND s.status = :status")
    List<Spot> findAllInViewport(@Param("minLat") double minLat, @Param("maxLat") double maxLat,
                                  @Param("minLng") double minLng, @Param("maxLng") double maxLng,
                                  @Param("status") SpotStatus status);
}
