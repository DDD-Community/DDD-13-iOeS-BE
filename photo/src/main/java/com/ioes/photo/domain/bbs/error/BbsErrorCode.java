package com.ioes.photo.domain.bbs.error;

import com.ioes.photo.global.error.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 게시판 도메인 에러 코드 정의.
 *
 * @author 황제연
 */
@Getter
@RequiredArgsConstructor
public enum BbsErrorCode implements ErrorCode {

    POST_NOT_FOUND("B001", "게시글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
