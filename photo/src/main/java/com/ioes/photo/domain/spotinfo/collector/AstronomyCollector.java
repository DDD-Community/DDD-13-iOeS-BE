package com.ioes.photo.domain.spotinfo.collector;

import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spotinfo.service.CollectResult;
import com.ioes.photo.domain.spotinfo.service.SpotInfoUpdateService;
import com.ioes.photo.external.astronomy.AstronomyApiClient;
import com.ioes.photo.external.astronomy.dto.SunMoonRiseSetResponse;
import com.ioes.photo.external.astronomy.dto.SunMoonRiseSetResponse.Item;
import com.ioes.photo.global.common.util.NullUtils;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 한국천문연구원 출몰시각 수집기.
 *
 * 이 이슈 스코프에서는 대한민국 전 지역이 서울 기준과 거의 동일하다는 가정 하에
 * "서울" 1회 조회 후 모든 PUBLISHED 스팟에 공통 적용한다.
 * 전국 확장 시 spot 별 지역 매핑으로 전환한다.
 *
 * @author 김성민
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AstronomyCollector {

    private static final String DEFAULT_LOCATION = "서울";
    private static final DateTimeFormatter LOCDATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");

    private final SpotRepository spotRepository;
    private final SpotInfoUpdateService spotInfoUpdateService;
    private final AstronomyApiClient astronomyApiClient;

    public CollectResult collect() {
        LocalDate today = LocalDate.now();
        SunMoonRiseSetResponse response;
        try {
            response = astronomyApiClient.getRiseSetInfo(today.format(LOCDATE_FORMAT), DEFAULT_LOCATION);
        } catch (Exception e) {
            log.error("[AstronomyCollector] API 호출 실패 location={} reason={}",
                DEFAULT_LOCATION, e.getMessage());
            return new CollectResult(0, 0);
        }

        LocalTime sunrise;
        LocalTime sunset;
        try {
            Item item = extractItem(response);
            sunrise = parseTime(item.trimmedSunrise());
            sunset = parseTime(item.trimmedSunset());
        } catch (Exception e) {
            log.error("[AstronomyCollector] 응답 파싱 실패 reason={}", e.getMessage());
            return new CollectResult(0, 0);
        }

        List<Spot> targets = spotRepository.findAllByStatus(SpotStatus.PUBLISHED);
        int success = 0;
        int fail = 0;
        for (Spot spot : targets) {
            try {
                spotInfoUpdateService.upsertAstronomy(spot.getId(), today, sunrise, sunset);
                success++;
            } catch (Exception e) {
                log.warn("[AstronomyCollector] upsert 실패 spotId={} reason={}",
                    spot.getId(), e.getMessage());
                fail++;
            }
        }
        return new CollectResult(success, fail);
    }

    private Item extractItem(SunMoonRiseSetResponse response) {
        if (response.body() == null
            || response.body().items() == null
            || response.body().items().item() == null
            || response.body().items().item().isEmpty()) {
            throw new IllegalStateException("출몰시각 item 비어있음");
        }
        return response.body().items().item().get(0);
    }

    private LocalTime parseTime(String hhmm) {
        if (NullUtils.isBlank(hhmm)) {
            return null;
        }
        return LocalTime.parse(hhmm, TIME_FORMAT);
    }
}
