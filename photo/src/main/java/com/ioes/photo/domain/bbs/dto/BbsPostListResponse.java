package com.ioes.photo.domain.bbs.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ioes.photo.domain.bbs.entity.BbsPost;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/**
 * 게시판 목록 조회 응답 DTO.
 *
 * @author 황제연
 */
@Schema(description = "게시판 목록 조회 응답")
public record BbsPostListResponse(
    @Schema(description = "게시글 목록") List<BbsPostItem> items,
    @Schema(description = "현재 페이지 번호 (0부터 시작)") int page,
    @Schema(description = "다음 페이지 존재 여부") boolean hasNext
) {

    @Schema(description = "게시글 항목")
    public record BbsPostItem(
        @Schema(description = "게시글 번호") Long postId,
        @Schema(description = "게시글 제목") String title,
        @Schema(description = "게시글 내용") String content,
        @Schema(description = "작성일") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate createdAt,
        @Schema(description = "상단 고정 여부") boolean pinned
    ) {
        public static BbsPostItem from(BbsPost post) {
            return new BbsPostItem(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt().toLocalDate(),
                post.isPinned()
            );
        }
    }
}
