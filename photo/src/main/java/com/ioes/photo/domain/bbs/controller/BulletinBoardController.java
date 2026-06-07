package com.ioes.photo.domain.bbs.controller;

import com.ioes.photo.domain.bbs.dto.BbsPostDetailResponse;
import com.ioes.photo.domain.bbs.dto.BbsPostListResponse;
import com.ioes.photo.domain.bbs.service.BulletinBoardService;
import com.ioes.photo.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게시판(BBS) 컨트롤러.
 *
 * masterId로 게시판 종류를 구분한다 (1: 공지사항).
 * 비로그인 사용자도 조회 가능하다.
 *
 * @author 황제연
 */
@Tag(name = "게시판", description = "게시판(공지사항 등) 조회 API")
@RestController
@RequestMapping("/v1/bbs")
@RequiredArgsConstructor
public class BulletinBoardController {

    private final BulletinBoardService bulletinBoardService;

    @Operation(summary = "게시글 목록 조회", description = "게시판 목록을 조회합니다. 고정 공지가 최신순으로 상단에 표시됩니다. 페이지당 20개.")
    @SecurityRequirements
    @GetMapping("/posts")
    public ApiResponse<BbsPostListResponse> getPosts(
        @Parameter(description = "게시판 구분 번호 (1: 공지사항)", required = true)
        @RequestParam Long masterId,
        @Parameter(description = "페이지 번호 (0부터 시작)")
        @RequestParam(defaultValue = "0") int page
    ) {
        return ApiResponse.success(bulletinBoardService.getPosts(masterId, page));
    }

    @Operation(summary = "게시글 상세 조회", description = "게시글 상세 내용을 조회합니다.")
    @SecurityRequirements
    @GetMapping("/posts/{postId}")
    public ApiResponse<BbsPostDetailResponse> getPostDetail(
        @Parameter(description = "게시글 번호", required = true) @PathVariable Long postId,
        @Parameter(description = "게시판 구분 번호 (1: 공지사항)", required = true) @RequestParam Long masterId
    ) {
        return ApiResponse.success(bulletinBoardService.getPostDetail(masterId, postId));
    }
}
