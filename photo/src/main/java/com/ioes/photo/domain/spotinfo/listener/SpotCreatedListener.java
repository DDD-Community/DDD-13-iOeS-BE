package com.ioes.photo.domain.spotinfo.listener;

import com.ioes.photo.domain.spot.event.SpotCreatedEvent;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spotinfo.collector.AstronomyCollector;
import com.ioes.photo.domain.spotinfo.collector.WeatherCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 스팟 생성 직후 날씨/천문 정보 초기 수집 리스너.
 *
 * 스케줄러 주기(날씨 3시간, 천문 1일)를 기다리지 않고 등록 시점에 채워
 * 신규 스팟의 정보 공백을 제거한다. 수집 실패는 등록 결과에 영향을 주지 않으며
 * 다음 스케줄 주기에 자동 보정된다.
 *
 * @author 김성민
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpotCreatedListener {

    private final SpotRepository spotRepository;
    private final WeatherCollector weatherCollector;
    private final AstronomyCollector astronomyCollector;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSpotCreated(SpotCreatedEvent event) {
        spotRepository.findById(event.spotId()).ifPresent(spot -> {
            try {
                weatherCollector.collectForSpot(spot);
            } catch (Exception e) {
                log.warn("[SpotCreatedListener] 날씨 초기 수집 실패 spotId={} reason={}",
                    spot.getId(), e.getMessage());
            }
            try {
                astronomyCollector.collectForSpot(spot.getId());
            } catch (Exception e) {
                log.warn("[SpotCreatedListener] 천문 초기 수집 실패 spotId={} reason={}",
                    spot.getId(), e.getMessage());
            }
        });
    }
}
