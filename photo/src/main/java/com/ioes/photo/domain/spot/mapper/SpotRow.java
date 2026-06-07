package com.ioes.photo.domain.spot.mapper;

/**
 * 스팟 리스트 조회 MyBatis 결과 행.
 *
 * @author 황제연
 */
public record SpotRow(
    Long id,
    String name,
    String theme,
    long bookmarkCount,
    Double distanceKm
) {}
