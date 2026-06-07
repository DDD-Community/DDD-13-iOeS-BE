package com.ioes.photo.domain.address.controller;

import com.ioes.photo.domain.address.dto.AddressSearchResponse;
import com.ioes.photo.domain.address.service.AddressSearchService;
import com.ioes.photo.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 주소 검색 컨트롤러.
 *
 * @author 김성민
 */
@Tag(name = "주소", description = "주소 검색 API")
@Validated
@RestController
@RequestMapping("/v1/address")
@RequiredArgsConstructor
public class AddressSearchController {

    private final AddressSearchService addressSearchService;

    @Operation(
        summary = "주소 검색",
        description = "검색어로 카카오 주소 검색 후보 목록을 페이징 조회합니다. 결과가 없으면 빈 배열을 반환합니다. 비로그인 허용."
    )
    @SecurityRequirements
    @GetMapping("/search")
    public ApiResponse<AddressSearchResponse> searchAddress(
        @Parameter(description = "검색어 (주소 키워드)", example = "서울 강남대로 396") @RequestParam @NotBlank String query,
        @Parameter(description = "페이지 번호 (1부터 시작)") @RequestParam(defaultValue = "1") @Min(1) int page,
        @Parameter(description = "페이지당 결과 수 (1~30)") @RequestParam(defaultValue = "10") @Min(1) @Max(30) int size
    ) {
        return ApiResponse.success(addressSearchService.searchAddress(query, page, size));
    }
}
