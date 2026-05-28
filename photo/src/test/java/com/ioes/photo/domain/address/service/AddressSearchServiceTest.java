package com.ioes.photo.domain.address.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.ioes.photo.domain.address.dto.AddressSearchResponse;
import com.ioes.photo.external.kakao.KakaoLocalApiClient;
import com.ioes.photo.external.kakao.dto.KakaoAddressSearch;
import com.ioes.photo.external.kakao.dto.KakaoAddressSearch.Item;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link AddressSearchService} 단위 테스트.
 *
 * @author 김성민
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AddressSearchService 단위 테스트")
class AddressSearchServiceTest {

    @Mock KakaoLocalApiClient kakaoLocalApiClient;

    @InjectMocks AddressSearchService addressSearchService;

    @Test
    @DisplayName("카카오 검색 결과를 응답 DTO로 변환하고 요청 페이지를 그대로 담는다")
    void mapsSearchResultToResponse() {
        KakaoAddressSearch search = new KakaoAddressSearch(
            List.of(new Item("서울 강남구 테헤란로 152", "서울 강남구 테헤란로 152", "서울 강남구 역삼동 737", 37.5, 127.03)),
            1, true
        );
        given(kakaoLocalApiClient.searchAddress(eq("강남"), anyInt(), anyInt())).willReturn(search);

        AddressSearchResponse response = addressSearchService.searchAddress("강남", 2, 10);

        assertThat(response.addresses()).hasSize(1);
        assertThat(response.addresses().get(0).addressName()).isEqualTo("서울 강남구 테헤란로 152");
        assertThat(response.addresses().get(0).latitude()).isEqualTo(37.5);
        assertThat(response.addresses().get(0).longitude()).isEqualTo(127.03);
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.isEnd()).isTrue();
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 목록을 반환한다")
    void returnsEmptyList_whenNoResult() {
        given(kakaoLocalApiClient.searchAddress(eq("없음"), anyInt(), anyInt()))
            .willReturn(new KakaoAddressSearch(List.of(), 0, true));

        AddressSearchResponse response = addressSearchService.searchAddress("없음", 1, 10);

        assertThat(response.addresses()).isEmpty();
        assertThat(response.totalCount()).isZero();
        assertThat(response.isEnd()).isTrue();
    }
}
