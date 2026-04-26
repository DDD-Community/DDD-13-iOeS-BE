package com.ioes.photo.domain.spotinfo.service;

import com.ioes.photo.domain.spotinfo.entity.SpotInfo;
import com.ioes.photo.domain.spotinfo.repository.SpotInfoRepository;
import com.ioes.photo.external.crowd.enums.CongestionLevel;
import com.ioes.photo.external.weather.enums.PrecipitationType;
import com.ioes.photo.external.weather.enums.SkyStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SpotInfo 영역별 upsert 서비스.
 *
 * 스케줄러가 스팟별로 호출하며, 각 메서드는 독립적인 트랜잭션을 갖는다.
 * 실패한 스팟이 있어도 다른 스팟 처리에 영향을 주지 않도록
 * 트랜잭션 범위를 스팟 단위로 좁혀 유지한다.
 *
 * @author 김성민
 */
@Service
@RequiredArgsConstructor
public class SpotInfoUpdateService {

    private final SpotInfoRepository spotInfoRepository;

    @Transactional
    public void upsertCrowd(Long spotId, CongestionLevel level, String message,
                            Integer populationMin, Integer populationMax,
                            LocalDateTime observedAt) {
        SpotInfo info = findOrCreate(spotId);
        info.updateCrowd(level, message, populationMin, populationMax, observedAt);
        spotInfoRepository.save(info);
    }

    @Transactional
    public void upsertWeather(Long spotId, SkyStatus sky, PrecipitationType precipitation,
                              Double temperature, LocalDateTime observedAt) {
        SpotInfo info = findOrCreate(spotId);
        info.updateWeather(sky, precipitation, temperature, observedAt);
        spotInfoRepository.save(info);
    }

    @Transactional
    public void upsertAstronomy(Long spotId, LocalDate date, LocalTime sunrise, LocalTime sunset) {
        SpotInfo info = findOrCreate(spotId);
        info.updateAstronomy(date, sunrise, sunset);
        spotInfoRepository.save(info);
    }

    private SpotInfo findOrCreate(Long spotId) {
        return spotInfoRepository.findById(spotId).orElseGet(() -> SpotInfo.create(spotId));
    }
}
