package com.ioes.photo.domain.myspot.controller;

import com.ioes.photo.domain.myspot.dto.CancelPublicationResponse;
import com.ioes.photo.domain.myspot.dto.CreateMySpotRequest;
import com.ioes.photo.domain.myspot.dto.CreateMySpotResponse;
import com.ioes.photo.domain.myspot.dto.MySpotListResponse;
import com.ioes.photo.domain.myspot.dto.OpenMySpotResponse;
import com.ioes.photo.domain.myspot.dto.ReleaseMySpotResponse;
import com.ioes.photo.domain.myspot.dto.UpdateMySpotRequest;
import com.ioes.photo.domain.myspot.dto.UpdateMySpotResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
            + "나만보기(DRAFT), 검수중(PENDING), 재검토대기(RE_REVIEW_PENDING), 공개(PUBLISHED), 반려(REJECTED) 상태를 모두 노출합니다. "
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
            + "등록 직후 상태는 DRAFT(나만보기)이며, 사용자가 '오픈하기'로 오픈 신청 시 검수(PENDING)가 시작됩니다."
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

    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema(type = "object"),
            schemaProperties = {
                @SchemaProperty(name = "request",
                    schema = @Schema(implementation = UpdateMySpotRequest.class)),
                @SchemaProperty(name = "image",
                    schema = @Schema(type = "string", format = "binary",
                        description = "교체할 스팟 이미지 파일. 미첨부 시 기존 이미지를 그대로 유지합니다."))
            },
            encoding = @Encoding(name = "request", contentType = MediaType.APPLICATION_JSON_VALUE)
        )
    )
    @Operation(
        summary = "나만의 스팟 수정",
        description = "나만보기(DRAFT) 또는 반려(REJECTED) 상태의 스팟을 수정합니다. "
            + "검수중이거나 공개된 스팟은 SP010으로 응답하므로, 공개를 먼저 해제한 뒤 수정해야 합니다. "
            + "반려된 스팟은 수정 후 오픈 신청으로 재검수를 요청할 수 있습니다. "
            + "좌표를 변경하면 주소·기상 격자·혼잡 지역과 날씨 정보가 새 위치 기준으로 다시 계산됩니다. "
            + "이미지를 첨부하지 않으면 기존 이미지가 유지되며, 수정으로 스팟 상태가 바뀌지는 않습니다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping(value = "/users/me/my-spots/{spotId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UpdateMySpotResponse> updateMySpot(
        @CurrentUserId Long userId,
        @PathVariable Long spotId,
        @RequestPart("request") @Valid UpdateMySpotRequest request,
        @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return ApiResponse.success(mySpotService.updateMySpot(userId, spotId, request, image));
    }

    @Operation(
        summary = "나만의 스팟 삭제",
        description = "본인이 등록한 스팟을 삭제합니다(논리삭제). "
            + "검수중(PENDING)/재검토대기(RE_REVIEW_PENDING) 상태에서는 SP011로 응답하므로, "
            + "오픈 신청을 먼저 철회한 뒤 삭제해야 합니다. "
            + "다른 사용자의 북마크 목록에는 '삭제된 스팟'으로 표시됩니다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/users/me/my-spots/{spotId}")
    public ApiResponse<Void> deleteMySpot(
        @CurrentUserId Long userId,
        @PathVariable Long spotId
    ) {
        mySpotService.deleteMySpot(userId, spotId);
        return ApiResponse.success();
    }

    @Operation(
        summary = "나만의 스팟 오픈 신청(검수 요청)",
        description = "나만보기(DRAFT) 또는 반려(REJECTED) 상태의 스팟을 지도에 공개하기 위해 오픈 신청합니다. "
            + "DRAFT는 검수중(PENDING)으로, REJECTED는 재검토대기(RE_REVIEW_PENDING)로 전이되며 검수가 시작됩니다. "
            + "본인이 등록한 스팟만 신청할 수 있습니다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/users/me/my-spots/{spotId}/open-requests")
    public ApiResponse<OpenMySpotResponse> requestOpen(
        @CurrentUserId Long userId,
        @PathVariable Long spotId
    ) {
        return ApiResponse.success(mySpotService.requestOpen(userId, spotId));
    }

    @Operation(
        summary = "나만의 스팟 공개 해제(오픈 신청 철회 / 비공개 전환)",
        description = "스팟의 공개 상태를 해제해 나만보기(DRAFT)로 되돌립니다. "
            + "검수중(PENDING)/재검토대기(RE_REVIEW_PENDING)이면 오픈 신청 철회로, 공개(PUBLISHED)면 비공개 전환으로 처리되며 "
            + "응답의 previousStatus 로 어느 쪽이었는지 구분할 수 있습니다. "
            + "이미 쌓인 좋아요/북마크는 그대로 유지됩니다. "
            + "나만보기(DRAFT) 상태는 해제할 대상이 없어 SP009, "
            + "철회 직전에 운영자 검수가 확정된 경우에는 SP009 대신 SP004(이미 처리된 신청이에요)로 응답합니다. "
            + "본인이 등록한 스팟만 해제할 수 있습니다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/users/me/my-spots/{spotId}/publications")
    public ApiResponse<CancelPublicationResponse> cancelPublication(
        @CurrentUserId Long userId,
        @PathVariable Long spotId
    ) {
        return ApiResponse.success(mySpotService.cancelPublication(userId, spotId));
    }

    @Operation(
        summary = "나만의 스팟 노출 켜기",
        description = "공개(PUBLISHED) 상태인 스팟의 지도뷰/리스트 노출을 켭니다. "
            + "검수 flow(status)와는 독립적인 별도 플래그라 상태는 그대로 PUBLISHED로 유지됩니다. "
            + "PUBLISHED가 아니면 SP012로 응답하며, 이미 켜져 있어도 그대로 성공 응답합니다. "
            + "본인이 등록한 스팟만 처리할 수 있습니다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/users/me/my-spots/{spotId}/releases")
    public ApiResponse<ReleaseMySpotResponse> releaseSpot(
        @CurrentUserId Long userId,
        @PathVariable Long spotId
    ) {
        return ApiResponse.success(mySpotService.releaseSpot(userId, spotId));
    }

    @Operation(
        summary = "나만의 스팟 노출 끄기",
        description = "공개(PUBLISHED) 상태인 스팟의 지도뷰/리스트 노출을 끕니다. "
            + "검수 flow(status)와는 독립적인 별도 플래그라 상태는 그대로 PUBLISHED로 유지되며, 재검수 없이 다시 켤 수 있습니다. "
            + "PUBLISHED가 아니면 SP012로 응답하며, 이미 꺼져 있어도 그대로 성공 응답합니다. "
            + "본인이 등록한 스팟만 처리할 수 있습니다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/users/me/my-spots/{spotId}/releases")
    public ApiResponse<ReleaseMySpotResponse> unreleaseSpot(
        @CurrentUserId Long userId,
        @PathVariable Long spotId
    ) {
        return ApiResponse.success(mySpotService.unreleaseSpot(userId, spotId));
    }
}
