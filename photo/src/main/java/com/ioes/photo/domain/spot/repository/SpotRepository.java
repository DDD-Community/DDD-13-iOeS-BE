package com.ioes.photo.domain.spot.repository;

import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
