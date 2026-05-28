package com.ioes.photo.external.kakao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 카카오 로컬 주소 검색(search/address) JSON 응답 매핑 DTO.
 *
 * @author 김성민
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchAddressResponse(
    Meta meta,
    List<Document> documents
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(
        @JsonProperty("total_count") int totalCount,
        @JsonProperty("is_end") boolean isEnd
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Document(
        @JsonProperty("address_name") String addressName,
        String x,
        String y,
        @JsonProperty("road_address") RoadAddress roadAddress,
        @JsonProperty("address") Address address
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RoadAddress(
        @JsonProperty("address_name") String addressName
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Address(
        @JsonProperty("address_name") String addressName
    ) {}
}
