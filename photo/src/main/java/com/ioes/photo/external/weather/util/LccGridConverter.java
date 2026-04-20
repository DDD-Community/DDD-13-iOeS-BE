package com.ioes.photo.external.weather.util;

/**
 * 기상청 단기예보 LCC (Lambert Conformal Conic) 격자 변환 유틸.
 *
 * 위경도 좌표를 기상청 단기예보 API가 요구하는 격자 좌표(nx, ny)로 변환한다.
 * 공식 값은 기상청 제공 변환식 상수를 그대로 사용한다.
 *
 * @author 김성민
 */
public final class LccGridConverter {

    private static final double RE = 6371.00877;
    private static final double GRID = 5.0;
    private static final double SLAT1 = 30.0;
    private static final double SLAT2 = 60.0;
    private static final double OLON = 126.0;
    private static final double OLAT = 38.0;
    private static final double XO = 43.0;
    private static final double YO = 136.0;

    private static final double DEGRAD = Math.PI / 180.0;

    private LccGridConverter() {
    }

    public static GridPoint toGrid(double latitude, double longitude) {
        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.log(Math.cos(slat1) / Math.cos(slat2))
            / Math.log(Math.tan(Math.PI * 0.25 + slat2 * 0.5)
            / Math.tan(Math.PI * 0.25 + slat1 * 0.5));
        double sf = Math.pow(Math.tan(Math.PI * 0.25 + slat1 * 0.5), sn)
            * Math.cos(slat1) / sn;
        double ro = re * sf / Math.pow(Math.tan(Math.PI * 0.25 + olat * 0.5), sn);

        double ra = re * sf / Math.pow(Math.tan(Math.PI * 0.25 + latitude * DEGRAD * 0.5), sn);
        double theta = longitude * DEGRAD - olon;
        if (theta > Math.PI) {
            theta -= 2.0 * Math.PI;
        }
        if (theta < -Math.PI) {
            theta += 2.0 * Math.PI;
        }
        theta *= sn;

        int nx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);
        return new GridPoint(nx, ny);
    }

    public record GridPoint(int nx, int ny) {}
}
