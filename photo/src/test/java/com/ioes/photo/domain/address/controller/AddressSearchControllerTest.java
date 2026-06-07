package com.ioes.photo.domain.address.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ioes.photo.domain.address.dto.AddressItem;
import com.ioes.photo.domain.address.dto.AddressSearchResponse;
import com.ioes.photo.domain.address.service.AddressSearchService;
import com.ioes.photo.global.common.response.ApiResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link AddressSearchController} 단위 테스트.
 *
 * @author 김성민
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AddressSearchController 단위 테스트")
class AddressSearchControllerTest {

    @Mock AddressSearchService addressSearchService;

    @InjectMocks AddressSearchController addressSearchController;

    @Test
    @DisplayName("서비스 응답을 ApiResponse.success로 감싸서 반환한다")
    void wrapsServiceResponseInApiResponse() {
        AddressSearchResponse serviceResponse = new AddressSearchResponse(
            List.of(new AddressItem("서울 강남구 테헤란로 152", "서울 강남구 테헤란로 152", "서울 강남구 역삼동 737", 37.5, 127.03)),
            1, 1, true
        );
        given(addressSearchService.searchAddress(eq("강남"), anyInt(), anyInt())).willReturn(serviceResponse);

        ApiResponse<AddressSearchResponse> response = addressSearchController.searchAddress("강남", 1, 10);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isSameAs(serviceResponse);
        assertThat(response.getData().addresses().get(0).addressName()).isEqualTo("서울 강남구 테헤란로 152");
        then(addressSearchService).should().searchAddress("강남", 1, 10);
    }
}
