package com.ioes.photo.domain.bbs.service;

import com.ioes.photo.domain.bbs.dto.BbsPostDetailResponse;
import com.ioes.photo.domain.bbs.dto.BbsPostListResponse;
import com.ioes.photo.domain.bbs.dto.BbsPostListResponse.BbsPostItem;
import com.ioes.photo.domain.bbs.entity.BbsPost;
import com.ioes.photo.domain.bbs.error.BbsErrorCode;
import com.ioes.photo.domain.bbs.repository.BbsPostRepository;
import com.ioes.photo.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 게시판(BBS) 서비스.
 *
 * 정렬 우선순위: 고정 공지(최신순) → 일반 공지(최신순)
 *
 * @author 황제연
 */
@Service
@RequiredArgsConstructor
public class BulletinBoardService {

    private static final int PAGE_SIZE = 20;

    private final BbsPostRepository bbsPostRepository;

    @Transactional(readOnly = true)
    public BbsPostListResponse getPosts(Long masterId, int page) {
        PageRequest pageable = PageRequest.of(page, PAGE_SIZE,
            Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("createdAt")));

        Page<BbsPost> result = bbsPostRepository.findByMasterId(masterId, pageable);
        List<BbsPostItem> items = result.getContent().stream()
            .map(BbsPostItem::from)
            .toList();

        return new BbsPostListResponse(items, page, result.hasNext());
    }

    @Transactional(readOnly = true)
    public BbsPostDetailResponse getPostDetail(Long masterId, Long postId) {
        BbsPost post = bbsPostRepository.findByIdAndMasterId(postId, masterId)
            .orElseThrow(() -> new BusinessException(BbsErrorCode.POST_NOT_FOUND));
        return BbsPostDetailResponse.from(post);
    }
}
