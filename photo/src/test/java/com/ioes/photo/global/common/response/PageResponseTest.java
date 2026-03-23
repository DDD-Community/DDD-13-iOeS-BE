package com.ioes.photo.global.common.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PageResponse} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("PageResponse 테스트")
class PageResponseTest {

    @Test
    @DisplayName("Page 객체로부터 PageResponse 변환 - 중간 페이지")
    void from_middlePage() {
        List<String> content = List.of("item1", "item2", "item3");
        PageRequest pageable = PageRequest.of(1, 3);
        Page<String> page = new PageImpl<>(content, pageable, 10L);

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.getContent()).containsExactlyElementsOf(content);
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(3);
        assertThat(response.getTotalElements()).isEqualTo(10L);
        assertThat(response.getTotalPages()).isEqualTo(4);
        assertThat(response.isLast()).isFalse();
    }

    @Test
    @DisplayName("마지막 페이지 여부 확인")
    void from_lastPage() {
        List<String> content = List.of("item1");
        PageRequest pageable = PageRequest.of(3, 3);
        Page<String> page = new PageImpl<>(content, pageable, 10L);

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.isLast()).isTrue();
        assertThat(response.getTotalPages()).isEqualTo(4);
    }

    @Test
    @DisplayName("빈 페이지 처리")
    void from_emptyPage() {
        Page<String> page = Page.empty(PageRequest.of(0, 10));

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isEqualTo(0L);
        assertThat(response.getTotalPages()).isEqualTo(0);
        assertThat(response.isLast()).isTrue();
    }

    @Test
    @DisplayName("첫 번째 페이지 처리")
    void from_firstPage() {
        List<Integer> content = List.of(1, 2, 5);
        PageRequest pageable = PageRequest.of(0, 5);
        Page<Integer> page = new PageImpl<>(content, pageable, 3L);

        PageResponse<Integer> response = PageResponse.from(page);

        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(5);
        assertThat(response.isLast()).isTrue();
    }
}