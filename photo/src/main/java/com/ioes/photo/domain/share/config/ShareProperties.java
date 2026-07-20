package com.ioes.photo.domain.share.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 공유 링크 설정 프로퍼티.
 *
 * @param baseUrl 공유 링크 외부 도메인 (og:url 생성에 사용)
 * @author 김성민
 */
@ConfigurationProperties(prefix = "app.share")
public record ShareProperties(
    String baseUrl
) {
}
