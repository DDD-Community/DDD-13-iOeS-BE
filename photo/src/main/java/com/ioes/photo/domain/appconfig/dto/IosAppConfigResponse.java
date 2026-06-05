package com.ioes.photo.domain.appconfig.dto;

import com.ioes.photo.domain.appconfig.config.IosAppConfigProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * iOS 앱 버전 설정 응답.
 *
 * @author 김성민
 */
@Schema(description = "iOS 앱 버전 설정 응답")
public record IosAppConfigResponse(
    @Schema(description = "이 버전 미만이면 앱 사용 불가", example = "1.3.0") String minimumVersion,
    @Schema(description = "앱스토어 기준 최신 버전", example = "1.5.0") String latestVersion,
    @Schema(description = "강제 업데이트 기능 on/off 스위치", example = "true") boolean forceUpdate,
    @Schema(description = "앱스토어 이동 URL", example = "https://apps.apple.com/app/id1234567890") String storeUrl
) {

    public static IosAppConfigResponse from(IosAppConfigProperties properties) {
        return new IosAppConfigResponse(
            properties.minimumVersion(),
            properties.latestVersion(),
            properties.forceUpdate(),
            properties.storeUrl()
        );
    }
}
