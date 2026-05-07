package com.ioes.photo.domain.spot.controller;

import com.ioes.photo.domain.spot.dto.SpotImageSyncRequest;
import com.ioes.photo.domain.spot.dto.SpotImageSyncResponse;
import com.ioes.photo.domain.spot.service.SpotImageAdminService;
import com.ioes.photo.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 스팟 어드민 컨트롤러.
 *
 * MVP 단계 운영 목적의 내부 전용 API를 제공한다.
 * JWT 인증이 필요하며 외부에 노출하지 않는다.
 *
 * @author 황제연
 */
@Tag(name = "스팟 어드민", description = "스팟 내부 운영 API")
@RestController
@RequestMapping("/internal/spots")
@RequiredArgsConstructor
public class SpotAdminController {

    private final SpotImageAdminService spotImageAdminService;

    @Operation(
        summary = "스팟 이미지 동기화",
        description = "S3에 직접 업로드된 이미지를 DB에 등록하고 썸네일을 생성합니다. 이미 등록된 경우 덮어씁니다."
    )
    @PostMapping("/{spotId}/image/sync")
    public ApiResponse<SpotImageSyncResponse> syncImage(
        @PathVariable Long spotId,
        @RequestBody @Valid SpotImageSyncRequest request
    ) {
        return ApiResponse.success(spotImageAdminService.syncImage(spotId, request));
    }
}