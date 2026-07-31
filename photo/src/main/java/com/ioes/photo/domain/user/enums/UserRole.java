package com.ioes.photo.domain.user.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 권한 ENUM.
 *
 * USER_ADMIN(관리자), USER_CUSTOMER(일반 사용자)로 구분되며 DB에는 짧은 코드로 저장된다.
 *
 * @author 황제연
 */
@Getter
@RequiredArgsConstructor
public enum UserRole implements CodedEnum {
    USER_ADMIN("A"),
    USER_CUSTOMER("C");

    public static final String ROLE_PREFIX = "ROLE_";

    private final String code;

    public String authority() {
        return ROLE_PREFIX + name();
    }
}
