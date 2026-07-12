package com.ioes.photo.domain.appconfig.controller;

import com.ioes.photo.domain.appconfig.config.AndroidAppConfigProperties;
import com.ioes.photo.domain.appconfig.config.IosAppConfigProperties;
import com.ioes.photo.domain.appconfig.dto.AndroidAppConfigResponse;
import com.ioes.photo.domain.appconfig.dto.IosAppConfigResponse;
import com.ioes.photo.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 앱 설정 조회 컨트롤러.
 *
 * @author 김성민
 */
@Tag(name = "앱 설정", description = "앱 버전 설정 조회 API")
@RestController
@RequestMapping("/v1/app/config")
@RequiredArgsConstructor
public class AppConfigController {

    private final IosAppConfigProperties iosAppConfigProperties;
    private final AndroidAppConfigProperties androidAppConfigProperties;

    @Operation(
        summary = "iOS 앱 버전 설정 조회",
        description = "iOS 앱의 최소 지원 버전, 최신 버전, 강제 업데이트 여부, 앱스토어 URL을 조회합니다. 비로그인 허용."
    )
    @SecurityRequirements
    @GetMapping("/ios")
    public ApiResponse<IosAppConfigResponse> getIosAppConfig() {
        return ApiResponse.success(IosAppConfigResponse.from(iosAppConfigProperties));
    }

    @Operation(
        summary = "Android 앱 버전 설정 조회",
        description = "Android 앱의 최소 지원 버전, 최신 버전, 강제 업데이트 여부, 플레이스토어 URL을 조회합니다. 비로그인 허용."
    )
    @SecurityRequirements
    @GetMapping("/android")
    public ApiResponse<AndroidAppConfigResponse> getAndroidAppConfig() {
        return ApiResponse.success(AndroidAppConfigResponse.from(androidAppConfigProperties));
    }
}
