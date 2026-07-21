package com.ioes.photo.domain.appconfig.dto;

import com.ioes.photo.domain.appconfig.config.AppConfigProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 앱 버전 설정 응답.
 *
 * @author 김성민
 */
@Schema(description = "앱 버전 설정 응답")
public record AppConfigResponse(
    @Schema(description = "이 버전 미만이면 앱 사용 불가", example = "1.3.0") String minimumVersion,
    @Schema(description = "스토어 기준 최신 버전", example = "1.5.0") String latestVersion,
    @Schema(description = "강제 업데이트 기능 on/off 스위치", example = "true") boolean forceUpdate,
    @Schema(description = "스토어 이동 URL", example = "https://apps.apple.com/app/id1234567890") String storeUrl,
    @Schema(description = "문의 이메일", example = "pickflow.help@gmail.com") String supportEmail,
    @Schema(description = "약관/정책 문서 목록") List<TermsPolicyResponse> termsPolicies
) {

    public static AppConfigResponse from(AppConfigProperties.PlatformConfig platformConfig) {
        return new AppConfigResponse(
            platformConfig.minimumVersion(),
            platformConfig.latestVersion(),
            platformConfig.forceUpdate(),
            platformConfig.storeUrl(),
            platformConfig.supportEmail(),
            platformConfig.termsPolicies().stream()
                .map(TermsPolicyResponse::from)
                .toList()
        );
    }

    /**
     * 약관/정책 문서 응답.
     */
    @Schema(description = "약관/정책 문서")
    public record TermsPolicyResponse(
        @Schema(description = "문서 구분 코드", example = "TERMS_OF_SERVICE") String type,
        @Schema(description = "문서 제목", example = "서비스 이용약관") String title,
        @Schema(description = "문서 URL", example = "https://example.com/terms") String url
    ) {

        public static TermsPolicyResponse from(AppConfigProperties.TermsPolicy termsPolicy) {
            return new TermsPolicyResponse(
                termsPolicy.type(),
                termsPolicy.title(),
                termsPolicy.url()
            );
        }
    }
}
