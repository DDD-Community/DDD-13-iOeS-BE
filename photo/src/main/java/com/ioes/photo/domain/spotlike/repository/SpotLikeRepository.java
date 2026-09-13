package com.ioes.photo.domain.spotlike.repository;

import com.ioes.photo.domain.spotlike.entity.SpotLike;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 스팟 좋아요 JPA 리포지토리.
 *
 * findByUserIdAndSpotId 는 @SQLRestriction 에 의해 활성(deleted_at IS NULL) 레코드만 반환한다.
 * 취소했던 좋아요를 되살리려면 native query 로 논리삭제 레코드까지 포함해 조회한다.
 *
 * @author 황제연
 */
public interface SpotLikeRepository extends JpaRepository<SpotLike, Long> {

    Optional<SpotLike> findByUserIdAndSpotId(Long userId, Long spotId);

    @Query(
        value = "SELECT * FROM spot_likes WHERE user_id = :userId AND spot_id = :spotId",
        nativeQuery = true
    )
    Optional<SpotLike> findByUserIdAndSpotIdIncludingDeleted(
        @Param("userId") Long userId,
        @Param("spotId") Long spotId
    );

    @Modifying
    @Query(
        value = """
            UPDATE spot_likes SET deleted_at = NULL
             WHERE user_id = :userId AND spot_id = :spotId AND deleted_at IS NOT NULL
            """,
        nativeQuery = true
    )
    int restoreLike(@Param("userId") Long userId, @Param("spotId") Long spotId);

    @Modifying
    @Query(
        value = """
            UPDATE spot_likes SET deleted_at = CURRENT_TIMESTAMP
             WHERE user_id = :userId AND spot_id = :spotId AND deleted_at IS NULL
            """,
        nativeQuery = true
    )
    int softDeleteLike(@Param("userId") Long userId, @Param("spotId") Long spotId);

    @Query("SELECT l.spotId FROM SpotLike l WHERE l.userId = :userId AND l.spotId IN :spotIds")
    Set<Long> findLikedSpotIds(@Param("userId") Long userId, @Param("spotIds") Collection<Long> spotIds);
}
