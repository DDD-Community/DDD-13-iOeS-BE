package com.ioes.photo.global.config.image;

import com.ioes.photo.global.config.image.ImageMagickConfig.ImageMagickCommand;
import com.ioes.photo.global.config.image.VipsConfig.VipsCommand;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link NativeToolMetricsConfig} 단위 테스트.
 *
 * 두 네이티브 도구 모두 미설치(absent) 상태로 구성해, 게이지 값이 checkAlive() 결과(0)를
 * 그대로 반영하는지만 확인한다. 설치된 환경에서의 1 값은 VipsCommandTest/ImageMagickCommandTest의
 * checkAlive() 테스트가 이미 검증한다.
 *
 * @author 황제연
 */
@DisplayName("NativeToolMetricsConfig 단위 테스트")
class NativeToolMetricsConfigTest {

    @Test
    @DisplayName("미설치 상태의 두 도구를 tool 태그가 다른 게이지 두 개로 등록하고 값은 0이다")
    void registersGaugesReflectingAbsentState() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        new NativeToolMetricsConfig(registry, VipsCommand.absent(), ImageMagickCommand.absent());

        double vipsValue = registry.get("native_tool_up")
            .tag("tool", NativeToolMetricsConfig.TOOL_VIPS).gauge().value();
        double imageMagickValue = registry.get("native_tool_up")
            .tag("tool", NativeToolMetricsConfig.TOOL_IMAGEMAGICK).gauge().value();

        assertThat(vipsValue).isEqualTo(0.0);
        assertThat(imageMagickValue).isEqualTo(0.0);
    }
}
