package com.ioes.photo.domain.spot.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


/**
 * 스팟 매퍼
 *
 * @author 황제연
 */
@Mapper
public interface SpotMapper {

    List<SpotRow> findSpots(
        @Param("status") String status,
        @Param("themes") List<String> themes,
        @Param("regionIds") List<Long> regionIds,
        @Param("userId") Long userId,
        @Param("lat") Double lat,
        @Param("lng") Double lng,
        @Param("offset") int offset,
        @Param("size") int size,
        @Param("sort") String sort
    );

    long countSpots(
        @Param("status") String status,
        @Param("themes") List<String> themes,
        @Param("regionIds") List<Long> regionIds
    );

    List<SpotViewportRow> findSpotsInViewport(
        @Param("minLat") double minLat,
        @Param("maxLat") double maxLat,
        @Param("minLng") double minLng,
        @Param("maxLng") double maxLng,
        @Param("status") String status,
        @Param("themes") List<String> themes,
        @Param("regionIds") List<Long> regionIds
    );

    SpotPreviewRow findSpotPreview(
        @Param("spotId") Long spotId,
        @Param("lat") Double lat,
        @Param("lng") Double lng
    );
}
