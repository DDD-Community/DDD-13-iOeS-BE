package com.ioes.photo.domain.statistics.notion;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 노션 연동 설정. token/database-id는 환경변수로 주입한다.
 *
 * @author 김성민
 */
@ConfigurationProperties(prefix = "app.notion")
public record NotionProperties(String token, String databaseId) {

    public boolean isConfigured() {
        return token != null && !token.isBlank() && databaseId != null && !databaseId.isBlank();
    }
}
