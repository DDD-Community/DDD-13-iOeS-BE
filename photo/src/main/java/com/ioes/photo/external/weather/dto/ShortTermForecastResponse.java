package com.ioes.photo.external.weather.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

/**
 * 기상청 단기예보 XML 응답 매핑 DTO.
 *
 * <p>응답 항목(Item)의 {@code category} 값으로 데이터 종류를 구분합니다.</p>
 * <ul>
 *   <li>TMP: 기온(℃)</li>
 *   <li>SKY: 하늘상태 (1 맑음, 3 구름많음, 4 흐림)</li>
 *   <li>POP: 강수확률(%)</li>
 *   <li>PTY: 강수형태 (0 없음, 1 비, 2 비/눈, 3 눈, 4 소나기)</li>
 *   <li>WSD: 풍속(m/s)</li>
 * </ul>
 *
 * @author 김성민
 */
@JacksonXmlRootElement(localName = "response")
public record ShortTermForecastResponse(
    Header header,
    Body body
) {
    public record Header(
        String resultCode,
        String resultMsg
    ) {}

    public record Body(
        String dataType,
        Items items,
        Integer numOfRows,
        Integer pageNo,
        Integer totalCount
    ) {}

    public record Items(
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "item")
        List<Item> item
    ) {}

    public record Item(
        String baseDate,
        String baseTime,
        String category,
        String fcstDate,
        String fcstTime,
        String fcstValue,
        int nx,
        int ny
    ) {}
}
