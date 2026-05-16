package com.ioes.photo.global.error.exception;

import com.ioes.photo.domain.user.error.UserErrorCode;
import lombok.Getter;

/**
 * 탈퇴한 계정으로 소셜 로그인 시도 시 발생하는 예외.
 *
 * restoreToken을 포함하여 클라이언트가 계정 복구 API를 호출할 수 있도록 한다.
 *
 * @author 황제연
 */
@Getter
public class AccountDeletedException extends BusinessException {

    private final String restoreToken;

    public AccountDeletedException(String restoreToken) {
        super(UserErrorCode.ACCOUNT_DELETED);
        this.restoreToken = restoreToken;
    }
}
