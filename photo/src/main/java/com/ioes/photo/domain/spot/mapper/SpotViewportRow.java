package com.ioes.photo.domain.spot.mapper;

/**
 * 뷰포트 조회 MyBatis 결과 행.
 *
 * @author 황제연
 */
public record SpotViewportRow(
    Long id,
    Double latitude,
    Double longitude,
    Long userId,
    Long regionId
) {}
