package com.ioes.photo.external.kakao.dto;

import com.ioes.photo.external.kakao.dto.Coord2AddressResponse.Address;
import com.ioes.photo.external.kakao.dto.Coord2AddressResponse.Document;
import com.ioes.photo.external.kakao.dto.Coord2AddressResponse.RoadAddress;
import java.util.Optional;

/**
 * 좌표 역지오코딩으로 확보한 정규화된 주소.
 *
 * @author 김성민
 */
public record KakaoAddress(
    String roadAddress,
    String jibunAddress,
    String simpleAddress
) {

    public static Optional<KakaoAddress> from(Coord2AddressResponse response) {
        if (response == null || response.documents() == null || response.documents().isEmpty()) {
            return Optional.empty();
        }
        Document document = response.documents().get(0);
        RoadAddress road = document.roadAddress();
        Address jibun = document.address();

        String roadAddress = road != null ? road.addressName() : null;
        String jibunAddress = jibun != null ? jibun.addressName() : null;
        String simpleAddress = resolveSimple(road, jibun);

        if (roadAddress == null && jibunAddress == null && simpleAddress == null) {
            return Optional.empty();
        }
        return Optional.of(new KakaoAddress(roadAddress, jibunAddress, simpleAddress));
    }

    private static String resolveSimple(RoadAddress road, Address jibun) {
        if (jibun != null) {
            return join(jibun.region1depthName(), jibun.region2depthName());
        }
        if (road != null) {
            return join(road.region1depthName(), road.region2depthName());
        }
        return null;
    }

    private static String join(String region1, String region2) {
        String first = region1 == null ? "" : region1.trim();
        String second = region2 == null ? "" : region2.trim();
        String joined = (first + " " + second).trim();
        return joined.isEmpty() ? null : joined;
    }
}
