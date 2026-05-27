package com.ioes.photo.domain.spot.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spotinfo.entity.SpotInfo;
import com.ioes.photo.external.crowd.enums.CongestionLevel;
import com.ioes.photo.external.weather.enums.PrecipitationType;
import com.ioes.photo.external.weather.enums.SkyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

/**
 * 스팟 상세 조회 응답.
 *
 * @author 김성민
 */
@Schema(description = "스팟 상세 조회 응답")
public record SpotDetailResponse(
    @Schema(description = "스팟 ID") Long spotId,
    @Schema(description = "스팟 이름") String name,
    @Schema(description = "한 줄 코멘트") String comment,
    @Schema(description = "테마 (SUNSET=노을, YUNSEUL=윤슬)") SpotTheme theme,
    @Schema(description = "위도") Double latitude,
    @Schema(description = "경도") Double longitude,
    @Schema(description = "간략 주소 (시·구 단위)") String address,
    @Schema(description = "도로명 주소") String addressRoad,
    @Schema(description = "지번 주소") String addressJibun,
    @Schema(description = "대표 이미지 URL") String imageUrl,
    @Schema(description = "촬영 일자", example = "2024-05-01") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate recordedDate,
    @Schema(description = "촬영 시각", example = "18:30") @JsonFormat(pattern = "HH:mm") LocalTime recordedTime,
    @Schema(description = "하늘 상태") SkyStatus weatherSky,
    @Schema(description = "강수 형태") PrecipitationType precipitation,
    @Schema(description = "강수 확률(%)") Integer precipitationProbability,
    @Schema(description = "혼잡도 수준") CongestionLevel congestionLevel,
    @Schema(description = "일몰 시각", example = "19:12") @JsonFormat(pattern = "HH:mm") LocalTime sunsetTime,
    @Schema(description = "천문 정보 기준일", example = "2024-05-01") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate astronomyDate,
    @Schema(description = "날씨 갱신 시각") LocalDateTime weatherUpdatedAt,
    @Schema(description = "혼잡도 갱신 시각") LocalDateTime congestionUpdatedAt,
    @Schema(description = "주차 정보") String parkingInfo,
    @Schema(description = "북마크 수") long bookmarkCount,
    @Schema(description = "북마크 여부 (비로그인 시 false)") boolean isBookmarked,
    @Schema(description = "내 스팟 여부 (비로그인 시 false)") boolean isMySpot
) {

    private static final String PARKING_INFO_DEFAULT = "정보 없음";

    public static SpotDetailResponse of(Spot spot, SpotImage spotImage, SpotInfo spotInfo,
                                        String imageUrl, boolean isBookmarked, boolean isMySpot) {
        return new SpotDetailResponse(
            spot.getId(),
            spot.getName(),
            spot.getComment(),
            spot.getTheme(),
            spot.getLatitude(),
            spot.getLongitude(),
            spot.getAddress(),
            spot.getAddressRoad(),
            spot.getAddressJibun(),
            imageUrl,
            Optional.ofNullable(spotImage).map(SpotImage::getRecordedDate).orElse(null),
            Optional.ofNullable(spotImage).map(SpotImage::getRecordedTime).orElse(null),
            Optional.ofNullable(spotInfo).map(SpotInfo::getWeatherSky).orElse(null),
            Optional.ofNullable(spotInfo).map(SpotInfo::getWeatherPrecipitation).orElse(null),
            Optional.ofNullable(spotInfo).map(SpotInfo::getPrecipitationProbability).orElse(null),
            Optional.ofNullable(spotInfo).map(SpotInfo::getCongestionLevel).orElse(null),
            Optional.ofNullable(spotInfo).map(SpotInfo::getSunsetTime).orElse(null),
            Optional.ofNullable(spotInfo).map(SpotInfo::getAstronomyDate).orElse(null),
            Optional.ofNullable(spotInfo).map(SpotInfo::getWeatherUpdatedAt).orElse(null),
            Optional.ofNullable(spotInfo).map(SpotInfo::getCongestionUpdatedAt).orElse(null),
            PARKING_INFO_DEFAULT,
            spot.getBookmarkCount(),
            isBookmarked,
            isMySpot
        );
    }
}
