package com.ioes.photo.domain.spot.error;

import com.ioes.photo.global.error.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 스팟 도메인 에러 코드.
 *
 * @author 황제연
 */
@Getter
@RequiredArgsConstructor
public enum SpotErrorCode implements ErrorCode {

    SPOT_NOT_FOUND("SP001", "존재하지 않는 스팟입니다.", HttpStatus.NOT_FOUND),
    SPOT_NOT_PUBLISHED("SP002", "아직 승인되지 않은 스팟입니다. 승인되지 않은 스팟은 신고할 수 없습니다.", HttpStatus.BAD_REQUEST),
    SPOT_DELETED("SP003", "삭제된 스팟은 북마크로 지정할 수 없습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
