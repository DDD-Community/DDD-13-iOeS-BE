package com.ioes.photo.domain.spot.mapper;

public record SpotRow(
    Long id,
    String name,
    String theme,
    Double distanceKm
) {}