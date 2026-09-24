package com.ioes.photo.domain.spotlike.error;

import com.ioes.photo.global.error.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 스팟 좋아요 도메인 에러 코드.
 *
 * @author 황제연
 */
@Getter
@RequiredArgsConstructor
public enum SpotLikeErrorCode implements ErrorCode {

    ALREADY_LIKED("SL001", "이미 좋아요한 스팟이에요.", HttpStatus.CONFLICT),
    NOT_LIKED("SL002", "좋아요하지 않은 스팟이에요.", HttpStatus.BAD_REQUEST),
    SPOT_NOT_LIKEABLE("SL003", "공개된 스팟에만 좋아요를 누를 수 있어요.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
