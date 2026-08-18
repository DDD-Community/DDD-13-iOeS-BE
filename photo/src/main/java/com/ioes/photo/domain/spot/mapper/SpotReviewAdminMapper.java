package com.ioes.photo.domain.spot.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 어드민 검수 목록 MyBatis 매퍼.
 *
 * 정렬/검색/상태필터/페이징을 서버에서 동적으로 처리한다.
 *
 * @author 황제연
 */
@Mapper
public interface SpotReviewAdminMapper {

    List<AdminSpotRow> findReviewSpots(
        @Param("status") String status,
        @Param("q") String q,
        @Param("offset") int offset,
        @Param("size") int size
    );

    long countReviewSpots(
        @Param("status") String status,
        @Param("q") String q
    );
}
