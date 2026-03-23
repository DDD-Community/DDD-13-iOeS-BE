package com.ioes.photo.global.config.security.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Spring Security 허용 URL 설정 프로퍼티 레코드.
 *
 * @param publicUrls 인증 없이 접근 가능한 URL 패턴 목록
 * @author 황제연
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
    List<String> publicUrls
) {}
