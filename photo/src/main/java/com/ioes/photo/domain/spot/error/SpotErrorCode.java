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
    SPOT_DELETED("SP003", "삭제된 스팟은 북마크로 지정할 수 없습니다.", HttpStatus.BAD_REQUEST),
    SPOT_ALREADY_REVIEWED("SP004", "이미 처리된 스팟입니다.", HttpStatus.CONFLICT),
    SPOT_NOT_OPENABLE("SP005", "오픈 신청할 수 없는 상태의 스팟입니다.", HttpStatus.BAD_REQUEST),
    SPOT_REJECTION_REASON_REQUIRED("SP006", "반려 사유를 선택해야 합니다.", HttpStatus.BAD_REQUEST),
    SPOT_REJECTION_DETAIL_REQUIRED("SP007", "기타 사유는 상세 설명을 입력해야 합니다.", HttpStatus.BAD_REQUEST),
    SPOT_ACCESS_DENIED("SP008", "본인이 등록한 스팟만 처리할 수 있습니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
