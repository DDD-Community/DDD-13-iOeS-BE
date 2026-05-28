package com.ioes.photo.domain.myspot.controller;

import com.ioes.photo.domain.myspot.dto.CreateMySpotRequest;
import com.ioes.photo.domain.myspot.dto.CreateMySpotResponse;
import com.ioes.photo.domain.myspot.dto.MySpotListResponse;
import com.ioes.photo.domain.myspot.service.MySpotService;
import com.ioes.photo.global.auth.CurrentUserId;
import com.ioes.photo.global.common.response.ApiResponse;
import com.ioes.photo.global.common.validation.Latitude;
import com.ioes.photo.global.common.validation.Longitude;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 나만의 스팟 컨트롤러.
 *
 * @author 김성민
 */
@Tag(name = "나만의 스팟", description = "사용자가 등록한 스팟 조회/등록 API")
@Validated
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class MySpotController {

    private final MySpotService mySpotService;

    @Operation(
        summary = "나만의 스팟 목록 조회",
        description = "사용자가 등록한 스팟 목록을 6개 단위로 페이징 조회합니다. "
            + "검수 대기(PENDING), 공개(PUBLISHED), 반려(REJECTED) 상태를 모두 노출합니다. "
            + "위도/경도를 함께 전달하면 거리(km)도 함께 반환됩니다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/users/me/my-spots")
    public ApiResponse<MySpotListResponse> getMySpots(
        @CurrentUserId Long userId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(required = false) @Latitude Double latitude,
        @RequestParam(required = false) @Longitude Double longitude
    ) {
        return ApiResponse.success(mySpotService.findMySpots(userId, page, latitude, longitude));
    }

    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema(type = "object"),
            schemaProperties = {
                @SchemaProperty(name = "request",
                    schema = @Schema(implementation = CreateMySpotRequest.class)),
                @SchemaProperty(name = "image",
                    schema = @Schema(type = "string", format = "binary", description = "스팟 이미지 파일"))
            },
            encoding = @Encoding(name = "request", contentType = MediaType.APPLICATION_JSON_VALUE)
        )
    )
    @Operation(
        summary = "나만의 스팟 등록",
        description = "이미지 파일과 메타데이터(JSON)를 multipart로 전송해 스팟을 등록합니다. "
            + "서버가 이미지를 S3에 업로드하고 썸네일을 생성하며, 위·경도로 주소를 역지오코딩합니다. "
            + "등록 직후 상태는 PENDING이며 운영자 검수 후 PUBLISHED/REJECTED로 전이됩니다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(value = "/users/me/my-spots", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateMySpotResponse> createMySpot(
        @CurrentUserId Long userId,
        @RequestPart("request") @Valid CreateMySpotRequest request,
        @RequestPart("image") MultipartFile image
    ) {
        return ApiResponse.success(mySpotService.createMySpot(userId, request, image));
    }
}
