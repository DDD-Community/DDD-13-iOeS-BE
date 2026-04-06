package com.ioes.photo.external.astronomy.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

/**
 * 한국천문연구원 출몰시각 XML 응답 매핑 DTO.
 *
 * <p>응답 시간값에 공백 패딩이 포함되어 있으므로 (예: "0747  "),
 * {@code trimmed*()} 메서드를 통해 정제된 값을 사용해야 합니다.</p>
 *
 * @author 김성민
 */
@JacksonXmlRootElement(localName = "response")
public record SunMoonRiseSetResponse(
    Header header,
    Body body
) {
    public record Header(
        String resultCode,
        String resultMsg
    ) {}

    public record Body(
        Items items,
        int numOfRows,
        int pageNo,
        int totalCount
    ) {}

    public record Items(
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "item")
        List<Item> item
    ) {}

    public record Item(
        String locdate,
        String location,
        String longitude,
        String longitudeNum,
        String latitude,
        String latitudeNum,
        String sunrise,
        String suntransit,
        String sunset,
        String moonrise,
        String moontransit,
        String moonset,
        String civilm,
        String civile,
        String nautm,
        String naute,
        String astm,
        String aste
    ) {
        /**
         * 응답값의 공백 패딩을 제거한 일출 시각 반환
         */
        public String trimmedSunrise() {
            return sunrise != null ? sunrise.trim() : null;
        }

        public String trimmedSunset() {
            return sunset != null ? sunset.trim() : null;
        }

        public String trimmedMoonrise() {
            return moonrise != null ? moonrise.trim() : null;
        }

        public String trimmedMoonset() {
            return moonset != null ? moonset.trim() : null;
        }

        public String trimmedCivilm() {
            return civilm != null ? civilm.trim() : null;
        }

        public String trimmedCivile() {
            return civile != null ? civile.trim() : null;
        }
    }
}
