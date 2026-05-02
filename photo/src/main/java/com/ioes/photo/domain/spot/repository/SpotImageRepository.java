package com.ioes.photo.domain.spot.repository;

import com.ioes.photo.domain.spot.entity.SpotImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpotImageRepository extends JpaRepository<SpotImage, Long> {

    List<SpotImage> findAllBySpotIdIn(List<Long> spotIds);

    @Modifying
    @Query("UPDATE SpotImage si SET si.thumbnailKey = :thumbnailKey WHERE si.spotId = :spotId AND si.thumbnailKey IS NULL")
    int updateThumbnailKeyIfAbsent(@Param("spotId") Long spotId, @Param("thumbnailKey") String thumbnailKey);
}
