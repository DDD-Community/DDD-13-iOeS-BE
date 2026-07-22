package com.ioes.photo.global.config.security.properties;

import java.util.Collections;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Security 허용 URL 설정 프로퍼티 레코드.
 *
 * @param publicUrls    method 무관하게 인증 없이 접근 가능한 URL 패턴 목록
 * @param publicGetUrls GET 요청에 한해 인증 없이 접근 가능한 URL 패턴 목록
 * @author 황제연
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
    List<String> publicUrls,
    List<String> publicGetUrls
) {

    public SecurityProperties {
        publicUrls = publicUrls == null ? Collections.emptyList() : publicUrls;
        publicGetUrls = publicGetUrls == null ? Collections.emptyList() : publicGetUrls;
    }
}
