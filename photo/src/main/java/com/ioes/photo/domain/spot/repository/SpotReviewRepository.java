package com.ioes.photo.domain.spot.repository;

import com.ioes.photo.domain.spot.entity.SpotReview;
import com.ioes.photo.domain.spot.enums.ReviewDecision;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 스팟 검수 이력 JPA 리포지토리.
 *
 * @author 황제연
 */
public interface SpotReviewRepository extends JpaRepository<SpotReview, Long> {

    List<SpotReview> findBySpotIdAndDecisionOrderByCreatedAtDesc(Long spotId, ReviewDecision decision);
}
