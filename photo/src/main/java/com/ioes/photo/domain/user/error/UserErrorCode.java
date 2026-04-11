package com.ioes.photo.domain.user.error;

import com.ioes.photo.global.error.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 사용자 도메인 에러 코드 정의.
 *
 * - U001: 닉네임 자동 생성 실패 (500)
 *
 * @author 황제연
 */
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    NICKNAME_GENERATION_FAILED("U001", "닉네임을 생성할 수 없습니다. 관리자에게 문의해주세요.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;
}