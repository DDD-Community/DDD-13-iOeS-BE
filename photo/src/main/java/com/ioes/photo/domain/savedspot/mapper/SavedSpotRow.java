package com.ioes.photo.domain.savedspot.mapper;

import java.time.LocalDateTime;

/**
 * 저장된 스팟 목록 MyBatis 조회 결과 Row.
 *
 * @author 황제연
 */
public record SavedSpotRow(
    Long spotId,
    String name,
    String theme,
    Double latitude,
    Double longitude,
    Double distanceKm,
    long bookmarkCount,
    String status,
    LocalDateTime savedAt,
    boolean deleted
) {}
