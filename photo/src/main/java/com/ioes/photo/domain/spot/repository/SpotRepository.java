package com.ioes.photo.domain.spot.repository;

import com.ioes.photo.domain.spot.entity.Spot;
import java.util.List;
import java.util.Optional;

import com.ioes.photo.domain.spot.enums.SpotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 스팟 JPA 리포지토리.
 *
 * 외부 API 스케줄러의 수집 대상 조회를 위한 메서드들을 제공한다.
 *
 * findAllInViewport 개발 > 황제연
 *
 * @author 김성민, 황제연
 */
public interface SpotRepository extends JpaRepository<Spot, Long> {

    List<Spot> findAllByStatus(SpotStatus status);

    List<Spot> findAllByStatusAndCrowdAreaNameIsNotNull(SpotStatus status);

    List<Spot> findAllByStatusAndGridNxIsNotNullAndGridNyIsNotNull(SpotStatus status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Spot s SET s.bookmarkCount = s.bookmarkCount + 1 WHERE s.id = :spotId")
    void incrementBookmarkCount(@Param("spotId") Long spotId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Spot s SET s.bookmarkCount = s.bookmarkCount - 1 WHERE s.id = :spotId AND s.bookmarkCount > 0")
    void decrementBookmarkCount(@Param("spotId") Long spotId);

    @Query("SELECT s.bookmarkCount FROM Spot s WHERE s.id = :spotId")
    Optional<Long> findBookmarkCountById(@Param("spotId") Long spotId);

    @Query(value = "SELECT * FROM spots WHERE id = :spotId", nativeQuery = true)
    Optional<Spot> findByIdIncludingDeleted(@Param("spotId") Long spotId);

    @Query(
        value = "SELECT s.* FROM spots s " +
                "WHERE s.location && ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326) " +
                "AND s.status = :status " +
                "AND s.deleted_at IS NULL",
        nativeQuery = true
    )
    List<Spot> findAllInViewport(@Param("minLat") double minLat, @Param("maxLat") double maxLat,
                                  @Param("minLng") double minLng, @Param("maxLng") double maxLng,
                                  @Param("status") String status);
}
