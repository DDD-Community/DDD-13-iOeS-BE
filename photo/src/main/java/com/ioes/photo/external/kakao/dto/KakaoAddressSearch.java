package com.ioes.photo.external.kakao.dto;

import com.ioes.photo.external.kakao.dto.SearchAddressResponse.Address;
import com.ioes.photo.external.kakao.dto.SearchAddressResponse.Document;
import com.ioes.photo.external.kakao.dto.SearchAddressResponse.RoadAddress;
import java.util.List;

/**
 * 주소 검색으로 확보한 정규화된 주소 후보 목록.
 *
 * @author 김성민
 */
public record KakaoAddressSearch(
    List<Item> items,
    int totalCount,
    boolean isEnd
) {

    public record Item(
        String addressName,
        String roadAddress,
        String jibunAddress,
        Double latitude,
        Double longitude
    ) {}

    public static KakaoAddressSearch from(SearchAddressResponse response) {
        if (response == null) {
            return empty();
        }
        List<Item> items = response.documents() == null
            ? List.of()
            : response.documents().stream().map(KakaoAddressSearch::toItem).toList();
        int totalCount = response.meta() == null ? items.size() : response.meta().totalCount();
        boolean isEnd = response.meta() == null || response.meta().isEnd();
        return new KakaoAddressSearch(items, totalCount, isEnd);
    }

    private static Item toItem(Document document) {
        RoadAddress road = document.roadAddress();
        Address jibun = document.address();
        return new Item(
            document.addressName(),
            road != null ? road.addressName() : null,
            jibun != null ? jibun.addressName() : null,
            parseCoordinate(document.y()),
            parseCoordinate(document.x())
        );
    }

    private static Double parseCoordinate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static KakaoAddressSearch empty() {
        return new KakaoAddressSearch(List.of(), 0, true);
    }
}
