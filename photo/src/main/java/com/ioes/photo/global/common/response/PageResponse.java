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

    /*
        현재 페이지의 데이터 목록
     */
    private final List<T> content;

    /*
        현재 페이지 번호
     */
    private final int page;

    /*
        페이지 당 항목 수
     */
    private final int size;

    /*
        전체 항목 수
     */
    private final long totalElements;

    /*
        전체 페이지 수
     */
    private final int totalPages;

    /*
        마지막 페이지 여부
     */
    private final boolean last;

    /**
     * Spring Data Page 객체로부터 PageResponse를 생성합니다.
     *
     * @param <T>  항목의 데이터 타입
     * @param page Spring Data Page 객체
     * @return PageResponse 인스턴스
     */
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