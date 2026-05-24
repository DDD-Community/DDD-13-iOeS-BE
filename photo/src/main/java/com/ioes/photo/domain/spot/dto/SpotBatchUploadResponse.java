package com.ioes.photo.domain.spot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 스팟 배치 업로드 응답 DTO.
 *
 * @author 황제연
 */
@Schema(description = "스팟 배치 업로드 응답")
public record SpotBatchUploadResponse(
    @Schema(description = "총 처리 스팟 수") int total,
    @Schema(description = "성공 수") int success,
    @Schema(description = "실패 수") int failed,
    @Schema(description = "처리 결과 목록") List<SpotResult> results
) {

    @Schema(description = "스팟 처리 결과")
    public record SpotResult(
        @Schema(description = "스팟 ID") Long spotId,
        @Schema(description = "스팟 이름") String name,
        @Schema(description = "원본 이미지 URL") String imageUrl,
        @Schema(description = "썸네일 URL") String thumbnailUrl
    ) {
        public static SpotResult of(Long spotId, String name, String imageUrl, String thumbnailUrl) {
            return new SpotResult(spotId, name, imageUrl, thumbnailUrl);
        }
    }

    public static SpotBatchUploadResponse of(List<SpotResult> results) {
        return new SpotBatchUploadResponse(results.size(), results.size(), 0, results);
    }
}
