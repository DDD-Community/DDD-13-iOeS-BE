package com.ioes.photo.domain.spot.repository;

import com.ioes.photo.domain.spot.entity.SpotOpenRequest;
import com.ioes.photo.domain.spot.enums.SpotOpenRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 스팟 오픈 신청 이력 JPA 리포지토리.
 *
 * @author 황제연
 */
public interface SpotOpenRequestRepository extends JpaRepository<SpotOpenRequest, Long> {

    Optional<SpotOpenRequest> findFirstBySpotIdAndStatusOrderByRequestedAtDesc(
        Long spotId, SpotOpenRequestStatus status);

    List<SpotOpenRequest> findBySpotIdOrderByRequestedAtDesc(Long spotId);

    long countBySpotId(Long spotId);
}
