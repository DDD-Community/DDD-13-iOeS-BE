package com.ioes.photo.domain.spot.repository;

import com.ioes.photo.domain.spot.entity.Spot;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

import com.ioes.photo.domain.spot.enums.SpotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 스팟 JPA 리포지토리.
 *
 * 외부 API 스케줄러의 수집 대상 조회를 위한 메서드들을 제공한다.
 *
 * @author 김성민, 황제연
 */
public interface SpotRepository extends JpaRepository<Spot, Long> {

    List<Spot> findAllByStatus(SpotStatus status);

    List<Spot> findAllByStatusAndCrowdAreaNameIsNotNull(SpotStatus status);

    List<Spot> findAllByStatusAndGridNxIsNotNullAndGridNyIsNotNull(SpotStatus status);

    List<Spot> findAllByGridNxIsNullOrGridNyIsNull();

    @Modifying
    @Query("UPDATE Spot s SET s.bookmarkCount = s.bookmarkCount + 1 WHERE s.id = :spotId")
    void incrementBookmarkCount(@Param("spotId") Long spotId);

    @Modifying
    @Query("UPDATE Spot s SET s.bookmarkCount = s.bookmarkCount - 1 WHERE s.id = :spotId AND s.bookmarkCount > 0")
    void decrementBookmarkCount(@Param("spotId") Long spotId);

    @Modifying
    @Query("UPDATE Spot s SET s.viewCount = s.viewCount + 1 WHERE s.id = :spotId")
    void incrementViewCount(@Param("spotId") Long spotId);

    @Query("SELECT s.bookmarkCount FROM Spot s WHERE s.id = :spotId")
    Optional<Long> findBookmarkCountById(@Param("spotId") Long spotId);

    @Modifying
    @Query("UPDATE Spot s SET s.likeCount = s.likeCount + 1 WHERE s.id = :spotId")
    void incrementLikeCount(@Param("spotId") Long spotId);

    @Modifying
    @Query("UPDATE Spot s SET s.likeCount = s.likeCount - 1 WHERE s.id = :spotId AND s.likeCount > 0")
    void decrementLikeCount(@Param("spotId") Long spotId);

    @Query("SELECT s.likeCount FROM Spot s WHERE s.id = :spotId")
    Optional<Long> findLikeCountById(@Param("spotId") Long spotId);

    @Query(value = "SELECT * FROM spots WHERE id = :spotId", nativeQuery = true)
    Optional<Spot> findByIdIncludingDeleted(@Param("spotId") Long spotId);

    /**
     * 상태 전이(오픈 신청/철회/수정/삭제/검수) 경로 전용 조회.
     *
     * 사용자의 철회와 운영자의 승인·반려가 같은 행을 동시에 건드릴 수 있어,
     * 읽은 뒤 상태를 판단하는 구간을 행 잠금으로 직렬화한다.
     * 카운터 증감은 atomic UPDATE 로 처리하므로 이 잠금을 쓰지 않는다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Spot s WHERE s.id = :spotId")
    Optional<Spot> findWithLockById(@Param("spotId") Long spotId);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, SpotStatus status);
}
