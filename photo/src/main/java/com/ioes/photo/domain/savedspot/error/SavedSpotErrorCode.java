package com.ioes.photo.domain.savedspot.error;

import com.ioes.photo.global.error.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 저장된 스팟 도메인 에러 코드.
 *
 * @author 황제연
 */
@Getter
@RequiredArgsConstructor
public enum SavedSpotErrorCode implements ErrorCode {

    ALREADY_BOOKMARKED("SS001", "이미 북마크된 스팟입니다.", HttpStatus.CONFLICT),
    NOT_BOOKMARKED("SS002", "북마크되지 않은 스팟입니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
