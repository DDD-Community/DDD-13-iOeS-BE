package com.ioes.photo.domain.spotinfo.entity;

import com.ioes.photo.external.crowd.enums.CongestionLevel;
import com.ioes.photo.external.weather.enums.PrecipitationType;
import com.ioes.photo.external.weather.enums.SkyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 스팟별 외부 API 최신 스냅샷.
 *
 * 스케줄러가 혼잡도/날씨/천문 데이터를 영역별 주기로 덮어쓴다.
 * spots와 1:1 관계를 공유 PK(spot_id)로 표현한다.
 *
 * @author 김성민
 */
@Getter
@Entity
@Table(name = "spot_info")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotInfo {

    @Id
    @Column(name = "spot_id")
    private Long spotId;

    @Enumerated(EnumType.STRING)
    @Column(name = "congestion_level", length = 20)
    private CongestionLevel congestionLevel;

    @Column(name = "congestion_message", columnDefinition = "text")
    private String congestionMessage;

    @Column(name = "population_min")
    private Integer populationMin;

    @Column(name = "population_max")
    private Integer populationMax;

    @Column(name = "congestion_updated_at")
    private LocalDateTime congestionUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "weather_sky", length = 20)
    private SkyStatus weatherSky;

    @Enumerated(EnumType.STRING)
    @Column(name = "weather_precipitation", length = 20)
    private PrecipitationType weatherPrecipitation;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "weather_updated_at")
    private LocalDateTime weatherUpdatedAt;

    @Column(name = "astronomy_date")
    private LocalDate astronomyDate;

    @Column(name = "sunrise_time")
    private LocalTime sunriseTime;

    @Column(name = "sunset_time")
    private LocalTime sunsetTime;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private SpotInfo(Long spotId) {
        this.spotId = spotId;
    }

    public static SpotInfo create(Long spotId) {
        return new SpotInfo(spotId);
    }

    public void updateCrowd(CongestionLevel level, String message,
                            Integer populationMin, Integer populationMax,
                            LocalDateTime observedAt) {
        this.congestionLevel = level;
        this.congestionMessage = message;
        this.populationMin = populationMin;
        this.populationMax = populationMax;
        this.congestionUpdatedAt = observedAt;
    }

    public void updateWeather(SkyStatus sky, PrecipitationType precipitation,
                              Double temperature, LocalDateTime observedAt) {
        this.weatherSky = sky;
        this.weatherPrecipitation = precipitation;
        this.temperature = temperature;
        this.weatherUpdatedAt = observedAt;
    }

    public void updateAstronomy(LocalDate date, LocalTime sunrise, LocalTime sunset) {
        this.astronomyDate = date;
        this.sunriseTime = sunrise;
        this.sunsetTime = sunset;
    }
}
