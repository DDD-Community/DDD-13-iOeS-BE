package com.ioes.photo.domain.spot.repository;

import com.ioes.photo.domain.spot.entity.SpotReview;
import com.ioes.photo.domain.spot.enums.ReviewDecision;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 스팟 검수 이력 JPA 리포지토리.
 *
 * @author 황제연
 */
public interface SpotReviewRepository extends JpaRepository<SpotReview, Long> {

    List<SpotReview> findBySpotIdAndDecisionOrderByCreatedAtDesc(Long spotId, ReviewDecision decision);

    Optional<SpotReview> findFirstBySpotIdAndDecisionOrderByCreatedAtDesc(Long spotId, ReviewDecision decision);

    /**
     * 유저의 누적 검수 결정 이력 건수. spot_reviews는 append-only라 스팟이 이후 철회/삭제되어도 줄지 않는다.
     * spots에 걸린 @SQLRestriction(deleted_at IS NULL) 을 그대로 타면 삭제된 스팟의 이력이 누락되므로
     * native query로 우회한다.
     */
    @Query(value = "SELECT COUNT(*) FROM spot_reviews r JOIN spots s ON s.id = r.spot_id "
        + "WHERE s.user_id = :userId AND r.decision = :decisionCode", nativeQuery = true)
    long countByUserIdAndDecisionCode(@Param("userId") Long userId, @Param("decisionCode") String decisionCode);
}
