package com.ioes.photo.domain.savedspot.repository;

import com.ioes.photo.domain.savedspot.entity.SavedSpotArchive;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 저장된 스팟 아카이브 JPA 리포지토리.
 *
 * findByUserIdAndSpotId는 @SQLRestriction에 의해 활성(deleted_at IS NULL) 레코드만 반환한다.
 * 재활성화가 필요한 경우에는 native query로 soft-delete 레코드를 포함하여 조회한다.
 *
 * @author 황제연
 */
public interface SavedSpotArchiveRepository extends JpaRepository<SavedSpotArchive, Long> {

    Optional<SavedSpotArchive> findByUserIdAndSpotId(Long userId, Long spotId);

    @Query(
        value = "SELECT * FROM saved_spot_archives WHERE user_id = :userId AND spot_id = :spotId",
        nativeQuery = true
    )
    Optional<SavedSpotArchive> findByUserIdAndSpotIdIncludingDeleted(
        @Param("userId") Long userId,
        @Param("spotId") Long spotId
    );

    @Query("SELECT s.spotId FROM SavedSpotArchive s WHERE s.userId = :userId AND s.spotId IN :spotIds")
    Set<Long> findBookmarkedSpotIds(@Param("userId") Long userId, @Param("spotIds") Collection<Long> spotIds);

    long countByUserId(Long userId);
}
