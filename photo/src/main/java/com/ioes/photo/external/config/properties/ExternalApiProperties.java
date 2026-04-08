package com.ioes.photo.external.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 외부 API 연동 설정 프로퍼티 레코드.
 *
 * @param dataGoKr 공공데이터포털 (기상청, 천문연구원) 설정
 * @param seoul    서울 열린데이터광장 설정
 * @author 김성민
 */
@ConfigurationProperties(prefix = "app.external-api")
public record ExternalApiProperties(
    DataGoKr dataGoKr,
    Seoul seoul
) {
    public record DataGoKr(
        String baseUrl,
        String serviceKey
    ) {}

    public record Seoul(
        String baseUrl,
        String serviceKey
    ) {}
}
