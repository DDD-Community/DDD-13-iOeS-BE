package com.ioes.photo.global.common.util;

/**
 * 위경도 좌표 거리 계산 유틸리티.
 *
 * @author 김성민
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private GeoUtils() {
    }

    /**
     * 두 좌표 사이의 Haversine 거리(미터).
     */
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double sinLat = Math.sin(dLat / 2);
        double sinLon = Math.sin(dLon / 2);
        double a = sinLat * sinLat
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * sinLon * sinLon;
        return EARTH_RADIUS_METERS * 2 * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }
}
