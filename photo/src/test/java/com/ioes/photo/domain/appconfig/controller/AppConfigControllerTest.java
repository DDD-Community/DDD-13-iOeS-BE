package com.ioes.photo.domain.appconfig.controller;

import com.ioes.photo.domain.appconfig.config.IosAppConfigProperties;
import com.ioes.photo.domain.appconfig.dto.IosAppConfigResponse;
import com.ioes.photo.global.common.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        AppConfigController controller = new AppConfigController(createProperties());

        ApiResponse<IosAppConfigResponse> response = controller.getIosAppConfig();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().minimumVersion()).isEqualTo("1.3.0");
        assertThat(response.getData().latestVersion()).isEqualTo("1.5.0");
        assertThat(response.getData().forceUpdate()).isTrue();
        assertThat(response.getData().storeUrl()).isEqualTo("https://apps.apple.com/app/id1234567890");
    }

    @Test
    @DisplayName("설정된 문의 이메일과 약관/정책 목록을 그대로 반환한다")
    void shouldReturnConfiguredSupportEmailAndTermsPolicies() {
        AppConfigController controller = new AppConfigController(createProperties());

        ApiResponse<IosAppConfigResponse> response = controller.getIosAppConfig();

        assertThat(response.getData().supportEmail()).isEqualTo("pickflow.help@gmail.com");
        assertThat(response.getData().termsPolicies()).hasSize(2);
        assertThat(response.getData().termsPolicies().get(0).type()).isEqualTo("TERMS_OF_SERVICE");
        assertThat(response.getData().termsPolicies().get(0).title()).isEqualTo("서비스 이용약관");
        assertThat(response.getData().termsPolicies().get(0).url()).isEqualTo("https://example.com/terms");
        assertThat(response.getData().termsPolicies().get(1).type()).isEqualTo("PRIVACY_POLICY");
    }

    private IosAppConfigProperties createProperties() {
        return new IosAppConfigProperties(
            "1.3.0",
            "1.5.0",
            true,
            "https://apps.apple.com/app/id1234567890",
            "pickflow.help@gmail.com",
            List.of(
                new IosAppConfigProperties.TermsPolicy("TERMS_OF_SERVICE", "서비스 이용약관", "https://example.com/terms"),
                new IosAppConfigProperties.TermsPolicy("PRIVACY_POLICY", "개인정보처리방침", "https://example.com/privacy")
            )
        );
    }
}
