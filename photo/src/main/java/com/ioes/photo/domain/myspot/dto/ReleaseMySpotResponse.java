package com.ioes.photo.domain.myspot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 나만의 스팟 노출(release) on/off 응답 DTO.
 *
 * 검수 상태(status)는 그대로 PUBLISHED로 유지되며, released 값만 지도뷰/리스트 노출 여부를 나타낸다.
 *
 * @author 황제연
 */
@Schema(description = "나만의 스팟 노출 on/off 응답")
public record ReleaseMySpotResponse(
    @Schema(description = "스팟 ID") Long spotId,
    @Schema(description = "노출 여부 (true=지도뷰/리스트에 노출, false=비노출)") boolean released
) {}
