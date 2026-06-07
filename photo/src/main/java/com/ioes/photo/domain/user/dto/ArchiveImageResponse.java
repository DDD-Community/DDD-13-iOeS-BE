package com.ioes.photo.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 보관함 조회 응답 DTO.
 *
 * @param archiveName     보관함 이름
 * @param archiveImageUrl 보관함 이미지 Presigned URL (설정되지 않은 경우 null)
 * @author 황제연
 */
@Schema(description = "보관함 조회 응답")
public record ArchiveImageResponse(
    @Schema(description = "보관함 이름")
    String archiveName,
    @Schema(description = "보관함 이미지 Presigned URL. 이미지가 없으면 null")
    String archiveImageUrl
) {
    public static ArchiveImageResponse of(String archiveName, String presignedUrl) {
        return new ArchiveImageResponse(archiveName, presignedUrl);
    }
}
