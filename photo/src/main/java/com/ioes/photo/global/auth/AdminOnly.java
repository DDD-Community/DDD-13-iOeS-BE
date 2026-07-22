package com.ioes.photo.global.auth;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * USER_ADMIN 권한을 가진 로그인 사용자만 접근을 허용하는 메소드 보안 어노테이션.
 *
 * 컨트롤러(또는 서비스) 메소드/클래스에 부착하며, 권한이 없으면 AccessDeniedException(403)이 발생한다.
 * 활성화 조건: SecurityConfig의 @EnableMethodSecurity.
 * "USER_ADMIN" 권한명을 이 한 곳에만 두어 하드코딩 중복을 제거한다.
 *
 * @author 황제연
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasRole('USER_ADMIN')")
public @interface AdminOnly {
}
