package com.ioes.photo.domain.appconfig.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 플랫폼별 앱 버전 설정 프로퍼티 레코드.
 *
 * @param ios     iOS 앱 설정
 * @param android Android 앱 설정
 * @author 김성민
 */
@ConfigurationProperties(prefix = "app.app-config")
public record AppConfigProperties(
    PlatformConfig ios,
    PlatformConfig android
) {

    /**
     * 단일 플랫폼 앱 설정 프로퍼티 레코드.
     *
     * @param minimumVersion 이 버전 미만이면 앱 사용 불가
     * @param latestVersion  스토어 기준 최신 버전
     * @param forceUpdate    강제 업데이트 기능 on/off 스위치
     * @param storeUrl       스토어 이동 URL
     * @param supportEmail   문의 이메일
     * @param termsPolicies  약관/정책 문서 목록
     */
    public record PlatformConfig(
        String minimumVersion,
        String latestVersion,
        boolean forceUpdate,
        String storeUrl,
        String supportEmail,
        List<TermsPolicy> termsPolicies
    ) {
    }

    /**
     * 약관/정책 문서 프로퍼티 레코드.
     *
     * @param type  문서 구분 코드
     * @param title 문서 제목
     * @param url   문서 URL
     */
    public record TermsPolicy(
        String type,
        String title,
        String url
    ) {
    }
}
