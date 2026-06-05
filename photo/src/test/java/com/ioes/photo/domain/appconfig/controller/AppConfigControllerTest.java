package com.ioes.photo.domain.appconfig.controller;

import com.ioes.photo.domain.appconfig.config.IosAppConfigProperties;
import com.ioes.photo.domain.appconfig.dto.IosAppConfigResponse;
import com.ioes.photo.global.common.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AppConfigController} 단위 테스트.
 *
 * @author 김성민
 */
@DisplayName("AppConfigController 단위 테스트")
class AppConfigControllerTest {

    @Test
    @DisplayName("설정된 iOS 버전 정보를 그대로 반환한다")
    void shouldReturnConfiguredIosVersionInfo() {
        IosAppConfigProperties properties = new IosAppConfigProperties(
            "1.3.0", "1.5.0", true, "https://apps.apple.com/app/id1234567890"
        );
        AppConfigController controller = new AppConfigController(properties);

        ApiResponse<IosAppConfigResponse> response = controller.getIosAppConfig();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().minimumVersion()).isEqualTo("1.3.0");
        assertThat(response.getData().latestVersion()).isEqualTo("1.5.0");
        assertThat(response.getData().forceUpdate()).isTrue();
        assertThat(response.getData().storeUrl()).isEqualTo("https://apps.apple.com/app/id1234567890");
    }
}
