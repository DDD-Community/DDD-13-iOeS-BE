package com.ioes.photo.global.auth.oauth;

import com.ioes.photo.global.persistence.enumeration.CodedEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * OAuth 로그인 공급자 ENUM.
 *
 * @author 황제연
 */
@Getter
@RequiredArgsConstructor
public enum OAuthProvider implements CodedEnum {
    APPLE("A"),
    KAKAO("K");

    private final String code;
}
