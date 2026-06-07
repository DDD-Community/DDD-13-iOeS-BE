package com.ioes.photo.domain.bbs.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ioes.photo.domain.bbs.entity.BbsPost;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 게시판 상세 조회 응답 DTO.
 *
 * @author 황제연
 */
@Schema(description = "게시판 상세 조회 응답")
public record BbsPostDetailResponse(
    @Schema(description = "게시판 구분 번호") Long masterId,
    @Schema(description = "게시글 번호") Long postId,
    @Schema(description = "게시글 제목") String title,
    @Schema(description = "작성일") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate createdAt,
    @Schema(description = "게시글 내용") String content
) {
    public static BbsPostDetailResponse from(BbsPost post) {
        return new BbsPostDetailResponse(
            post.getMasterId(),
            post.getId(),
            post.getTitle(),
            post.getCreatedAt().toLocalDate(),
            post.getContent()
        );
    }
}
