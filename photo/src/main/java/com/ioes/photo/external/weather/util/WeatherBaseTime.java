package com.ioes.photo.external.weather.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 기상청 단기예보 API의 base_date / base_time 해석 유틸.
 *
 * 기상청은 02/05/08/11/14/17/20/23시에 발표하며, 발표 직후 10분 정도 지나야 조회 가능하다.
 * 현재 시각을 기준으로 조회 가능한 가장 최근의 발표 시각을 계산한다.
 *
 * @author 김성민
 */
public final class WeatherBaseTime {

    private static final int[] PUBLISH_HOURS = {2, 5, 8, 11, 14, 17, 20, 23};
    private static final int AVAILABILITY_DELAY_MINUTES = 10;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private WeatherBaseTime() {
    }

    public static BaseInfo resolve(LocalDateTime now) {
        LocalDateTime available = now.minusMinutes(AVAILABILITY_DELAY_MINUTES);
        int hour = available.getHour();
        int selectedHour = -1;
        for (int publishHour : PUBLISH_HOURS) {
            if (publishHour <= hour) {
                selectedHour = publishHour;
            }
        }
        if (selectedHour < 0) {
            LocalDate yesterday = available.toLocalDate().minusDays(1);
            return new BaseInfo(yesterday, 23);
        }
        return new BaseInfo(available.toLocalDate(), selectedHour);
    }

    public record BaseInfo(LocalDate date, int hour) {
        public String baseDate() {
            return date.format(DATE_FORMAT);
        }

        public String baseTime() {
            return String.format("%02d00", hour);
        }
    }
}
