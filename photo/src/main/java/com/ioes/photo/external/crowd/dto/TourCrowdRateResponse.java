package com.ioes.photo.external.crowd.dto;

import java.util.List;

/**
 * 한국관광공사 관광지 집중률 예측 API JSON 응답 매핑 DTO.
 *
 * <p>관광지 단위로 향후 30일 날짜별 집중률(%)을 제공합니다.
 * cnctrRate 는 관광지 간 비교 가능한 0~100 예측 지수입니다.</p>
 *
 * @author 김성민
 */
public record TourCrowdRateResponse(Response response) {

    public record Response(
        Header header,
        Body body
    ) {}

    public record Header(
        String resultCode,
        String resultMsg
    ) {}

    public record Body(
        Items items,
        Integer numOfRows,
        Integer pageNo,
        Integer totalCount
    ) {}

    public record Items(
        List<Item> item
    ) {}

    public record Item(
        String baseYmd,
        String areaCd,
        String areaNm,
        String signguCd,
        String signguNm,
        String tAtsNm,
        String cnctrRate
    ) {}
}
