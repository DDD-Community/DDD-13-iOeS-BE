package com.ioes.photo.domain.myspot.mapper;

import java.time.LocalDateTime;

/**
 * 나만의 스팟 목록 MyBatis 조회 결과 Row.
 *
 * @author 김성민
 */
public record MySpotRow(
    Long spotId,
    String name,
    String theme,
    Double latitude,
    Double longitude,
    Double distanceKm,
    LocalDateTime createdAt,
    String status,
    long bookmarkCount
) {}
