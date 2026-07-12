package com.ioes.photo.domain.appconfig.dto;

import com.ioes.photo.domain.appconfig.config.AndroidAppConfigProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Android 앱 버전 설정 응답.
 *
 * @author 김성민
 */
@Schema(description = "Android 앱 버전 설정 응답")
public record AndroidAppConfigResponse(
    @Schema(description = "이 버전 미만이면 앱 사용 불가", example = "1.3.0") String minimumVersion,
    @Schema(description = "플레이스토어 기준 최신 버전", example = "1.5.0") String latestVersion,
    @Schema(description = "강제 업데이트 기능 on/off 스위치", example = "true") boolean forceUpdate,
    @Schema(description = "플레이스토어 이동 URL", example = "https://play.google.com/store/apps/details?id=com.example.app") String storeUrl,
    @Schema(description = "문의 이메일", example = "pickflow.help@gmail.com") String supportEmail,
    @Schema(description = "약관/정책 문서 목록") List<TermsPolicyResponse> termsPolicies
) {

    public static AndroidAppConfigResponse from(AndroidAppConfigProperties properties) {
        return new AndroidAppConfigResponse(
            properties.minimumVersion(),
            properties.latestVersion(),
            properties.forceUpdate(),
            properties.storeUrl(),
            properties.supportEmail(),
            properties.termsPolicies().stream()
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

        public static TermsPolicyResponse from(AndroidAppConfigProperties.TermsPolicy termsPolicy) {
            return new TermsPolicyResponse(
                termsPolicy.type(),
                termsPolicy.title(),
                termsPolicy.url()
            );
        }
    }
}
