package com.ioes.photo.domain.spot.dto;

import com.ioes.photo.domain.spot.enums.SpotTheme;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 스팟 어드민 배치 등록 요청 DTO.
 *
 * @author 황제연
 */
@Schema(description = "스팟 배치 등록 요청")
public record SpotAdminCreateRequest(
    @Schema(description = "등록할 스팟 목록 (1개 이상)")
    @NotEmpty @Valid List<Item> spots
) {

    @Schema(description = "개별 스팟 등록 정보")
    public record Item(
        @Schema(description = "스팟 이름", example = "한강 노을 포인트")
        @NotBlank @Size(max = 100) String name,

        @Schema(description = "스팟 설명")
        @Size(max = 500) String comment,

        @Schema(description = "테마 코드 (SS=노을, YS=윤슬, SL=햇살, NV=야경)")
        @NotNull SpotTheme theme,

        @Schema(description = "위도", example = "37.55")
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,

        @Schema(description = "경도", example = "126.99")
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,

        @Schema(description = "주소")
        @Size(max = 255) String address,

        @Schema(description = "기상청 격자 X (날씨 연동용, 선택 — 미입력 시 위경도로 자동 계산)")
        Integer gridNx,

        @Schema(description = "기상청 격자 Y (날씨 연동용, 선택 — 미입력 시 위경도로 자동 계산)")
        Integer gridNy,

        @Schema(description = "혼잡도 지역명 (혼잡도 연동용, 선택)")
        @Size(max = 50) String crowdAreaName
    ) {}
}
