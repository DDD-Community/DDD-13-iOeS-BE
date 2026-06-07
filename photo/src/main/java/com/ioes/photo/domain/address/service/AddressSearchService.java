package com.ioes.photo.domain.address.service;

import com.ioes.photo.domain.address.dto.AddressSearchResponse;
import com.ioes.photo.external.kakao.KakaoLocalApiClient;
import com.ioes.photo.external.kakao.dto.KakaoAddressSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 주소 검색 서비스.
 *
 * <p>카카오 로컬 주소 검색 결과를 응답 DTO로 변환한다.</p>
 *
 * @author 김성민
 */
@Service
@RequiredArgsConstructor
public class AddressSearchService {

    private final KakaoLocalApiClient kakaoLocalApiClient;

    public AddressSearchResponse searchAddress(String query, int page, int size) {
        KakaoAddressSearch result = kakaoLocalApiClient.searchAddress(query, page, size);
        return AddressSearchResponse.of(result, page);
    }
}
