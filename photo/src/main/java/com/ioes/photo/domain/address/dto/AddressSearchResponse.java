package com.ioes.photo.domain.address.dto;

import com.ioes.photo.external.kakao.dto.KakaoAddressSearch;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 주소 검색 응답.
 *
 * @author 김성민
 */
@Schema(description = "주소 검색 응답")
public record AddressSearchResponse(
    @Schema(description = "주소 후보 목록") List<AddressItem> addresses,
    @Schema(description = "현재 페이지 번호 (1부터 시작)") int page,
    @Schema(description = "검색된 전체 결과 수") int totalCount,
    @Schema(description = "마지막 페이지 여부") boolean isEnd
) {

    public static AddressSearchResponse of(KakaoAddressSearch search, int page) {
        List<AddressItem> addresses = search.items().stream()
            .map(AddressItem::from)
            .toList();
        return new AddressSearchResponse(addresses, page, search.totalCount(), search.isEnd());
    }
}
