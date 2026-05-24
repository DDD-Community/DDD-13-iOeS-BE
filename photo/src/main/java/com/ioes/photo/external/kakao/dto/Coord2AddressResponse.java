package com.ioes.photo.external.kakao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 카카오 로컬 좌표→주소 변환(coord2address) JSON 응답 매핑 DTO.
 *
 * @author 김성민
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Coord2AddressResponse(
    List<Document> documents
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Document(
        @JsonProperty("road_address") RoadAddress roadAddress,
        @JsonProperty("address") Address address
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RoadAddress(
        @JsonProperty("address_name") String addressName,
        @JsonProperty("region_1depth_name") String region1depthName,
        @JsonProperty("region_2depth_name") String region2depthName
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Address(
        @JsonProperty("address_name") String addressName,
        @JsonProperty("region_1depth_name") String region1depthName,
        @JsonProperty("region_2depth_name") String region2depthName
    ) {}
}
