package com.ioes.photo.domain.address.dto;

import com.ioes.photo.external.kakao.dto.KakaoAddressSearch;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 주소 검색 후보 단건.
 *
 * @author 김성민
 */
@Schema(description = "주소 검색 후보")
public record AddressItem(
    @Schema(description = "전체 주소 (대표)") String addressName,
    @Schema(description = "도로명 주소, 없으면 null") String roadAddress,
    @Schema(description = "지번 주소, 없으면 null") String jibunAddress,
    @Schema(description = "위도, 좌표 없으면 null") Double latitude,
    @Schema(description = "경도, 좌표 없으면 null") Double longitude
) {

    public static AddressItem from(KakaoAddressSearch.Item item) {
        return new AddressItem(
            item.addressName(),
            item.roadAddress(),
            item.jibunAddress(),
            item.latitude(),
            item.longitude()
        );
    }
}
