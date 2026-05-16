package com.ioes.photo.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 보관함 이름 수정 요청 DTO.
 *
 * @author 황제연
 */
@Schema(description = "보관함 이름 수정 요청")
public record UpdateArchiveNameRequest(
    @Schema(description = "변경할 보관함 이름 (최대 20자)")
    @NotBlank(message = "보관함 이름은 비어있을 수 없습니다.")
    @Size(max = 20, message = "보관함 이름은 20자를 초과할 수 없습니다.")
    String archiveName
) {}
