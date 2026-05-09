package com.ioes.photo.domain.savedspot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 북마크 지정/해제 응답 DTO.
 *
 * @author 황제연
 */
@Schema(description = "북마크 응답")
public record BookmarkResponse(
    @Schema(description = "북마크 지정/해제 후 현재 북마크 수") long bookmarkCount
) {}
