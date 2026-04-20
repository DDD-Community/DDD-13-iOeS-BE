package com.ioes.photo.domain.spotinfo.collector;

import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spotinfo.service.CollectResult;
import com.ioes.photo.domain.spotinfo.service.SpotInfoUpdateService;
import com.ioes.photo.external.weather.WeatherApiClient;
import com.ioes.photo.external.weather.dto.ShortTermForecastResponse;
import com.ioes.photo.external.weather.dto.ShortTermForecastResponse.Item;
import com.ioes.photo.external.weather.enums.PrecipitationType;
import com.ioes.photo.external.weather.enums.SkyStatus;
import com.ioes.photo.external.weather.util.WeatherBaseTime;
import com.ioes.photo.external.weather.util.WeatherBaseTime.BaseInfo;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 기상청 단기예보 수집기.
 *
 * gridNx/Ny 가 매핑된 PUBLISHED 스팟을 격자별로 묶어 단일 API 호출로 공유한다.
 * 격자 단위 실패는 격리된다.
 *
 * @author 김성민
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherCollector {

    private static final DateTimeFormatter FORECAST_TIMESTAMP =
        DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final String CATEGORY_SKY = "SKY";
    private static final String CATEGORY_PTY = "PTY";
    private static final String CATEGORY_TMP = "TMP";

    private final SpotRepository spotRepository;
    private final SpotInfoUpdateService spotInfoUpdateService;
    private final WeatherApiClient weatherApiClient;

    public CollectResult collect() {
        List<Spot> targets = spotRepository
            .findAllByStatusAndGridNxIsNotNullAndGridNyIsNotNull(SpotStatus.PUBLISHED);
        Map<GridKey, List<Spot>> grouped = targets.stream()
            .collect(Collectors.groupingBy(s -> new GridKey(s.getGridNx(), s.getGridNy())));

        BaseInfo base = WeatherBaseTime.resolve(LocalDateTime.now());

        int success = 0;
        int fail = 0;
        for (Map.Entry<GridKey, List<Spot>> entry : grouped.entrySet()) {
            GridKey grid = entry.getKey();
            List<Spot> spotsInGrid = entry.getValue();
            try {
                applyGrid(grid, spotsInGrid, base);
                success += spotsInGrid.size();
            } catch (Exception e) {
                log.warn("[WeatherCollector] failed grid=({},{}) size={} reason={}",
                    grid.nx(), grid.ny(), spotsInGrid.size(), e.getMessage());
                fail += spotsInGrid.size();
            }
        }
        return new CollectResult(success, fail);
    }

    private void applyGrid(GridKey grid, List<Spot> spots, BaseInfo base) {
        ShortTermForecastResponse response = weatherApiClient
            .getShortTermForecast(base.baseDate(), base.baseTime(), grid.nx(), grid.ny());
        List<Item> items = extractItems(response);
        Forecast forecast = buildForecast(items);
        LocalDateTime observedAt = forecast.observedAt();
        for (Spot spot : spots) {
            spotInfoUpdateService.upsertWeather(
                spot.getId(),
                forecast.sky(),
                forecast.precipitation(),
                forecast.temperature(),
                observedAt
            );
        }
    }

    private List<Item> extractItems(ShortTermForecastResponse response) {
        if (response.body() == null
            || response.body().items() == null
            || response.body().items().item() == null) {
            throw new IllegalStateException("예보 item 비어있음");
        }
        return response.body().items().item();
    }

    private Forecast buildForecast(List<Item> items) {
        LocalDateTime nearest = items.stream()
            .map(this::forecastTimestamp)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .min(Comparator.naturalOrder())
            .orElseThrow(() -> new IllegalStateException("유효한 fcstTime 없음"));

        SkyStatus sky = pickValue(items, CATEGORY_SKY, nearest)
            .map(SkyStatus::fromCode)
            .orElse(null);
        PrecipitationType pty = pickValue(items, CATEGORY_PTY, nearest)
            .map(PrecipitationType::fromCode)
            .orElse(null);
        Double temperature = pickValue(items, CATEGORY_TMP, nearest)
            .map(this::parseDouble)
            .orElse(null);

        return new Forecast(sky, pty, temperature, nearest);
    }

    private Optional<String> pickValue(List<Item> items, String category, LocalDateTime target) {
        return items.stream()
            .filter(i -> category.equals(i.category()))
            .filter(i -> forecastTimestamp(i).map(t -> t.equals(target)).orElse(false))
            .map(Item::fcstValue)
            .findFirst();
    }

    private Optional<LocalDateTime> forecastTimestamp(Item item) {
        if (item.fcstDate() == null || item.fcstTime() == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDateTime.parse(item.fcstDate() + item.fcstTime(), FORECAST_TIMESTAMP));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private record GridKey(int nx, int ny) {}

    private record Forecast(SkyStatus sky, PrecipitationType precipitation,
                            Double temperature, LocalDateTime observedAt) {}
}
