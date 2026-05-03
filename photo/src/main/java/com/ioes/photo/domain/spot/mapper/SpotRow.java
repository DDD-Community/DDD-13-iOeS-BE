package com.ioes.photo.domain.spot.mapper;

/**
 * mybatis 동적쿼리 DTO
 *
 * @author 황제연
 * @param id
 * @param name
 * @param address
 * @param theme
 * @param latitude
 * @param longitude
 * @param distanceKm
 */
public record SpotRow(
    Long id,
    String name,
    String address,
    String theme,
    Double latitude,
    Double longitude,
    Double distanceKm
) {}
