package com.ioes.photo.domain.savedspot.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 저장된 스팟 목록 MyBatis 매퍼.
 *
 * @author 황제연
 */
@Mapper
public interface SavedSpotMapper {

    List<SavedSpotRow> findSavedSpots(
        @Param("userId") Long userId,
        @Param("lat") Double lat,
        @Param("lng") Double lng,
        @Param("offset") int offset,
        @Param("size") int size
    );

    long countSavedSpots(@Param("userId") Long userId);
}
