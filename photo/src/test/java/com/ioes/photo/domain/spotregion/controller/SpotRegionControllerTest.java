package com.ioes.photo.domain.spotregion.controller;

import com.ioes.photo.domain.spotregion.dto.RegionListResponse;
import com.ioes.photo.domain.spotregion.dto.RegionListResponse.RegionItem;
import com.ioes.photo.domain.spotregion.service.SpotRegionQueryService;
import com.ioes.photo.global.common.response.ApiResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * {@link SpotRegionController} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpotRegionController 단위 테스트")
class SpotRegionControllerTest {

    @Mock SpotRegionQueryService spotRegionQueryService;

    @InjectMocks SpotRegionController spotRegionController;

    @Test
    @DisplayName("서비스 응답을 ApiResponse.success로 감싸서 반환한다")
    void wrapsServiceResponseInApiResponse() {
        RegionListResponse serviceResponse =
            new RegionListResponse(List.of(new RegionItem(1L, "서울"), new RegionItem(2L, "대전")));
        given(spotRegionQueryService.findActiveRegions()).willReturn(serviceResponse);

        ApiResponse<RegionListResponse> response = spotRegionController.getActiveRegions();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().regions()).hasSize(2);
        assertThat(response.getData().regions().get(0).regionName()).isEqualTo("서울");
    }
}
