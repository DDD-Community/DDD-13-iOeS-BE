package com.ioes.photo.external.crowd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 서울시 실시간 인구 데이터 JSON 응답 매핑 DTO.
 *
 * <p>서울시 주요 122개 장소의 실시간 혼잡도, 인구 현황,
 * 인구 예측 데이터를 포함합니다.</p>
 *
 * @author 김성민
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CrowdStatusResponse(
    @JsonProperty("CITYDATA")
    CityData cityData
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CityData(
        @JsonProperty("AREA_NM")
        String areaNm,

        @JsonProperty("AREA_CD")
        String areaCd,

        @JsonProperty("LIVE_PPLTN_STTS")
        List<LivePopulation> livePopulationStats
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LivePopulation(
        @JsonProperty("AREA_NM")
        String areaNm,

        @JsonProperty("AREA_CD")
        String areaCd,

        @JsonProperty("AREA_CONGEST_LVL")
        String congestionLevel,

        @JsonProperty("AREA_CONGEST_MSG")
        String congestionMessage,

        @JsonProperty("AREA_PPLTN_MIN")
        String populationMin,

        @JsonProperty("AREA_PPLTN_MAX")
        String populationMax,

        @JsonProperty("MALE_PPLTN_RATE")
        String maleRate,

        @JsonProperty("FEMALE_PPLTN_RATE")
        String femaleRate,

        @JsonProperty("PPLTN_RATE_0")
        String rate0s,

        @JsonProperty("PPLTN_RATE_10")
        String rate10s,

        @JsonProperty("PPLTN_RATE_20")
        String rate20s,

        @JsonProperty("PPLTN_RATE_30")
        String rate30s,

        @JsonProperty("PPLTN_RATE_40")
        String rate40s,

        @JsonProperty("PPLTN_RATE_50")
        String rate50s,

        @JsonProperty("PPLTN_RATE_60")
        String rate60s,

        @JsonProperty("PPLTN_RATE_70")
        String rate70s,

        @JsonProperty("PPLTN_TIME")
        String populationTime,

        @JsonProperty("FCST_YN")
        String forecastYn,

        @JsonProperty("FCST_PPLTN")
        List<ForecastPopulation> forecastPopulations
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ForecastPopulation(
        @JsonProperty("FCST_TIME")
        String forecastTime,

        @JsonProperty("FCST_CONGEST_LVL")
        String congestionLevel,

        @JsonProperty("FCST_PPLTN_MIN")
        String populationMin,

        @JsonProperty("FCST_PPLTN_MAX")
        String populationMax
    ) {}
}
