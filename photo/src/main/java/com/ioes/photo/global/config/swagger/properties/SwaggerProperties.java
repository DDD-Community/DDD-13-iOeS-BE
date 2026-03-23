package com.ioes.photo.global.config.swagger.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Swagger / OpenAPI 문서 설정 프로퍼티 레코드.
 *
 * @param title              Swagger UI에 표시될 API 제목
 * @param version            API 버전 문자열
 * @param description        API 설명
 * @param securitySchemeName JWT 보안 스키마 이름 (Swagger UI 자물쇠 아이콘에 표시)
 * @author 황제연
 */
@ConfigurationProperties(prefix = "app.swagger")
public record SwaggerProperties(
    String title,
    String version,
    String description,
    String securitySchemeName
) {}
