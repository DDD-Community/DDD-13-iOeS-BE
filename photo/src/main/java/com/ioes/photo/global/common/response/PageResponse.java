package com.ioes.photo.global.common.response;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

/**
 * 페이지네이션 응답 래퍼 클래스
 * Spring Data JPA의 Page 객체를 API 응답 형태로 변환합니다
 *
 * @param <T> 페이지 내 항목의 데이터 타입
 * @author 황제연
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PageResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean last;

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast()
        );
    }
}