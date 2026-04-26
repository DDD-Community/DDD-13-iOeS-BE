package com.ioes.photo.external.weather.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 기상청 단기예보 API의 base_date / base_time 해석 유틸.
 *
 * 기상청은 02/05/08/11/14/17/20/23시에 발표하며, 발표 직후 일정 시간이 지나야 조회 가능하다.
 * 발표 가용 지연(분)은 외부 입력으로 받아 운영 중 튜닝 가능하도록 한다.
 *
 * @author 김성민
 */
public final class WeatherBaseTime {

    private static final int[] PUBLISH_HOURS = {2, 5, 8, 11, 14, 17, 20, 23};
    private static final int DEFAULT_AVAILABILITY_DELAY_MINUTES = 10;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private WeatherBaseTime() {
    }

    public static BaseInfo resolve(LocalDateTime now) {
        return resolve(now, DEFAULT_AVAILABILITY_DELAY_MINUTES);
    }

    public static BaseInfo resolve(LocalDateTime now, int availabilityDelayMinutes) {
        LocalDateTime available = now.minusMinutes(availabilityDelayMinutes);
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
