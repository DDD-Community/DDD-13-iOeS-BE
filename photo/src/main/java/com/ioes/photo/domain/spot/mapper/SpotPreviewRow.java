package com.ioes.photo.domain.spot.mapper;

/**
 * 스팟 미리보기 MyBatis 결과 행.
 *
 * @author 황제연
 */
public record SpotPreviewRow(
    Long id,
    String name,
    String theme,
    Long userId,
    String status,
    long bookmarkCount,
    long likeCount,
    Double distanceKm,
    String addressSimple,
    String addressRoad,
    String addressJibun
) {}
