package com.ioes.photo.domain.appconfig.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * iOS 앱 버전 설정 프로퍼티 레코드.
 *
 * @param minimumVersion 이 버전 미만이면 앱 사용 불가
 * @param latestVersion  앱스토어 기준 최신 버전
 * @param forceUpdate    강제 업데이트 기능 on/off 스위치
 * @param storeUrl       앱스토어 이동 URL
 * @author 김성민
 */
@ConfigurationProperties(prefix = "app.app-config.ios")
public record IosAppConfigProperties(
    String minimumVersion,
    String latestVersion,
    boolean forceUpdate,
    String storeUrl
) {
}
