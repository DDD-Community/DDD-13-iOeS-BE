package com.ioes.photo.global.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 현재 로그인한 사용자의 ID를 컨트롤러 파라미터로 주입하는 어노테이션.
 *
 * 로그인 상태면 JWT에서 파싱한 userId Long를 주입하고,
 * 비로그인(익명) 상태면 null을 주입한다.</p>
 *
 * @author 황제연
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {
}
