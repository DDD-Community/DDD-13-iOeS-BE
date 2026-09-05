package com.ioes.photo.domain.notification.error;

import com.ioes.photo.global.error.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 알림(검수완료 히스토리) 도메인 에러 코드.
 *
 * @author 황제연
 */
@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    REVIEW_HISTORY_NOT_FOUND("NT001", "존재하지 않는 검수완료 히스토리입니다.", HttpStatus.NOT_FOUND),
    REVIEW_HISTORY_ACCESS_DENIED("NT002", "본인의 검수완료 히스토리만 처리할 수 있습니다.", HttpStatus.FORBIDDEN),
    REVIEW_HISTORY_REJECT_REASON_REQUIRED("NT003", "반려 히스토리에는 반려 사유가 필요합니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
