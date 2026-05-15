package com.ioes.photo.domain.user.controller;

import com.ioes.photo.domain.user.dto.ArchiveImageResponse;
import com.ioes.photo.domain.user.dto.UpdateArchiveNameRequest;
import com.ioes.photo.domain.user.service.UserArchiveService;
import com.ioes.photo.global.auth.CurrentUserId;
import com.ioes.photo.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 사용자 보관함 컨트롤러.
 *
 * @author 황제연
 */
@Tag(name = "보관함", description = "사용자 보관함 이미지 및 이름 관리 API")
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserArchiveController {

    private final UserArchiveService userArchiveService;

    @Operation(summary = "보관함 이미지 등록/변경", description = "보관함 이미지를 업로드합니다. 기존 이미지가 있으면 교체됩니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(value = "/me/archive", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ArchiveImageResponse> updateArchiveImage(
        @CurrentUserId Long userId,
        @Parameter(description = "보관함 이미지 파일")
        @RequestPart("archiveImage") MultipartFile archiveImage
    ) {
        return ApiResponse.success(userArchiveService.updateArchiveImage(userId, archiveImage));
    }

    @Operation(summary = "보관함 조회", description = "보관함 이름과 이미지의 Presigned URL을 반환합니다. 이미지가 없으면 archiveImageUrl은 null을 반환합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/me/archive")
    public ApiResponse<ArchiveImageResponse> getArchiveImage(@CurrentUserId Long userId) {
        return ApiResponse.success(userArchiveService.getArchiveImage(userId));
    }

    @Operation(summary = "보관함 이름 수정", description = "보관함 이름을 변경합니다. 최대 20자까지 입력 가능합니다.")
    @SecurityRequirement(name = "Bearer Authentication")
    @PatchMapping("/me/archive/name")
    public ApiResponse<ArchiveImageResponse> updateArchiveName(
        @CurrentUserId Long userId,
        @RequestBody @Valid UpdateArchiveNameRequest request
    ) {
        return ApiResponse.success(userArchiveService.updateArchiveName(userId, request.archiveName()));
    }
}
