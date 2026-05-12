package com.ioes.photo.domain.spot.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spotinfo.entity.SpotInfo;
import com.ioes.photo.external.crowd.enums.CongestionLevel;
import com.ioes.photo.external.weather.enums.PrecipitationType;
import com.ioes.photo.external.weather.enums.SkyStatus;
import java.time.LocalTime;
import java.util.Optional;

/**
 * 스팟 상세 조회 응답.
 *
 * @author 김성민
 */
public record SpotDetailResponse(
    Long spotId,
    String name,
    String comment,
    SpotTheme theme,
    Double latitude,
    Double longitude,
    String address,
    String imageUrl,
    @JsonFormat(pattern = "HH:mm") LocalTime recordedTime,
    SkyStatus weatherSky,
    PrecipitationType precipitation,
    Integer precipitationProbability,
    Double temperature,
    CongestionLevel congestionLevel,
    @JsonFormat(pattern = "HH:mm") LocalTime sunsetTime
) {

    public static SpotDetailResponse of(Spot spot, SpotImage spotImage, SpotInfo spotInfo, String imageUrl) {
        return new SpotDetailResponse(
            spot.getId(),
            spot.getName(),
            spot.getComment(),
            spot.getTheme(),
            spot.getLatitude(),
            spot.getLongitude(),
            spot.getAddress(),
            imageUrl,
            Optional.ofNullable(spotImage).map(SpotImage::getRecordedTime).orElse(null),
            Optional.ofNullable(spotInfo).map(SpotInfo::getWeatherSky).orElse(null),
            Optional.ofNullable(spotInfo).map(SpotInfo::getWeatherPrecipitation).orElse(null),
            Optional.ofNullable(spotInfo).map(SpotInfo::getPrecipitationProbability).orElse(null),
            Optional.ofNullable(spotInfo).map(SpotInfo::getTemperature).orElse(null),
            Optional.ofNullable(spotInfo).map(SpotInfo::getCongestionLevel).orElse(null),
            Optional.ofNullable(spotInfo).map(SpotInfo::getSunsetTime).orElse(null)
        );
    }
}
