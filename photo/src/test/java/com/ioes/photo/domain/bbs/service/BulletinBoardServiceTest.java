package com.ioes.photo.domain.bbs.service;

import com.ioes.photo.domain.bbs.dto.BbsPostDetailResponse;
import com.ioes.photo.domain.bbs.dto.BbsPostListResponse;
import com.ioes.photo.domain.bbs.entity.BbsPost;
import com.ioes.photo.domain.bbs.error.BbsErrorCode;
import com.ioes.photo.domain.bbs.repository.BbsPostRepository;
import com.ioes.photo.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * {@link BulletinBoardService} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BulletinBoardService 단위 테스트")
class BulletinBoardServiceTest {

    @Mock BbsPostRepository bbsPostRepository;

    @InjectMocks BulletinBoardService bulletinBoardService;

    private static final Long MASTER_ID = 1L;

    @Nested
    @DisplayName("getPosts()")
    class GetPosts {

        @Test
        @DisplayName("masterId에 해당하는 게시글 목록을 반환한다")
        void shouldReturnPostList_whenMasterIdMatches() {
            BbsPost pinned = buildPost(1L, "고정공지", true);
            BbsPost normal = buildPost(2L, "일반공지", false);
            Page<BbsPost> page = new PageImpl<>(List.of(pinned, normal));
            given(bbsPostRepository.findByMasterId(eq(MASTER_ID), any(Pageable.class))).willReturn(page);

            BbsPostListResponse response = bulletinBoardService.getPosts(MASTER_ID, 0);

            assertThat(response.items()).hasSize(2);
            assertThat(response.items().get(0).postId()).isEqualTo(1L);
            assertThat(response.items().get(0).content()).isEqualTo("내용");
            assertThat(response.items().get(0).pinned()).isTrue();
            assertThat(response.page()).isEqualTo(0);
            assertThat(response.hasNext()).isFalse();
        }

        @Test
        @DisplayName("게시글이 없으면 빈 목록을 반환한다")
        void shouldReturnEmpty_whenNoPost() {
            given(bbsPostRepository.findByMasterId(eq(MASTER_ID), any(Pageable.class)))
                .willReturn(Page.empty());

            BbsPostListResponse response = bulletinBoardService.getPosts(MASTER_ID, 0);

            assertThat(response.items()).isEmpty();
            assertThat(response.hasNext()).isFalse();
        }

        @Test
        @DisplayName("다음 페이지가 있으면 hasNext가 true이다")
        void shouldReturnHasNext_whenNextPageExists() {
            List<BbsPost> content = List.of(buildPost(1L, "제목", false));
            Page<BbsPost> page = new PageImpl<>(content, Pageable.ofSize(20), 21);
            given(bbsPostRepository.findByMasterId(eq(MASTER_ID), any(Pageable.class))).willReturn(page);

            BbsPostListResponse response = bulletinBoardService.getPosts(MASTER_ID, 0);

            assertThat(response.hasNext()).isTrue();
        }
    }

    @Nested
    @DisplayName("getPostDetail()")
    class GetPostDetail {

        @Test
        @DisplayName("masterId와 postId가 일치하는 게시글 상세를 반환한다")
        void shouldReturnDetail_whenFound() {
            BbsPost post = buildPost(1L, "공지사항 제목", false);
            ReflectionTestUtils.setField(post, "content", "공지사항 내용입니다.");
            ReflectionTestUtils.setField(post, "masterId", MASTER_ID);
            given(bbsPostRepository.findByIdAndMasterId(1L, MASTER_ID)).willReturn(Optional.of(post));

            BbsPostDetailResponse response = bulletinBoardService.getPostDetail(MASTER_ID, 1L);

            assertThat(response.postId()).isEqualTo(1L);
            assertThat(response.masterId()).isEqualTo(MASTER_ID);
            assertThat(response.title()).isEqualTo("공지사항 제목");
            assertThat(response.content()).isEqualTo("공지사항 내용입니다.");
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 POST_NOT_FOUND 예외를 던진다")
        void shouldThrow_whenNotFound() {
            given(bbsPostRepository.findByIdAndMasterId(99L, MASTER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> bulletinBoardService.getPostDetail(MASTER_ID, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(BbsErrorCode.POST_NOT_FOUND));
        }
    }

    private BbsPost buildPost(Long id, String title, boolean pinned) {
        BbsPost post = BbsPost.builder()
            .masterId(MASTER_ID)
            .title(title)
            .content("내용")
            .pinned(pinned)
            .build();
        ReflectionTestUtils.setField(post, "id", id);
        ReflectionTestUtils.setField(post, "createdAt", LocalDateTime.now());
        return post;
    }
}
