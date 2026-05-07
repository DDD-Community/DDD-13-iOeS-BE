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
        @Param("theme") String theme,
        @Param("lat") Double lat,
        @Param("lng") Double lng,
        @Param("offset") int offset,
        @Param("size") int size
    );

    long countSpots(
        @Param("status") String status,
        @Param("theme") String theme
    );
}
