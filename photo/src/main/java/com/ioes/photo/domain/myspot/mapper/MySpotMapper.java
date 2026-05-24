package com.ioes.photo.domain.myspot.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 나만의 스팟 목록 MyBatis 매퍼.
 *
 * @author 김성민
 */
@Mapper
public interface MySpotMapper {

    List<MySpotRow> findMySpots(
        @Param("userId") Long userId,
        @Param("lat") Double lat,
        @Param("lng") Double lng,
        @Param("offset") int offset,
        @Param("size") int size
    );

    long countMySpots(
        @Param("userId") Long userId
    );
}
