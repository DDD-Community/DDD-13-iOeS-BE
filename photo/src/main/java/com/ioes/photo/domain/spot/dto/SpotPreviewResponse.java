package com.ioes.photo.domain.spot.dto;

import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.global.common.annotation.TruncateDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 스팟 미리보기 조회 응답.
 *
 * @author 황제연
 */
@Schema(description = "스팟 미리보기 응답")
public record SpotPreviewResponse(
    @Schema(description = "스팟 ID") Long spotId,
    @Schema(description = "스팟명") String name,
    @Schema(description = "내 스팟 여부") boolean isMySpot,
    @Schema(description = "테마") SpotTheme theme,
    @Schema(description = "북마크 수") long bookmarkCount,
    @Schema(description = "사용자 위치 기준 거리(km), 위치 미전달 시 null") @TruncateDecimal Double distanceKm,
    @Schema(description = "스팟 썸네일 이미지 URL, 이미지 없을 시 null") String imageUrl,
    @Schema(description = "간략 주소 (시·구 단위)") String addressSimple,
    @Schema(description = "도로명 주소, 주소 스키마 재구성 전까지 null") String addressRoad,
    @Schema(description = "지번 주소, 주소 스키마 재구성 전까지 null") String addressJibun
) {}
