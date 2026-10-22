package com.ioes.photo.global.config.image;

import com.ioes.photo.global.config.image.ImageMagickConfig.ImageMagickCommand;
import com.ioes.photo.global.config.image.VipsConfig.VipsCommand;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.function.Predicate;
import org.springframework.context.annotation.Configuration;

/**
 * libvips(vipsthumbnail), ImageMagick처럼 JVM 밖에 설치된 네이티브 프로그램의 생존 여부를
 * Prometheus 메트릭으로 내보내는 설정.
 *
 * {@code /actuator/health}에는 올리지 않는다. 이 두 프로그램이 멈춰도 앱 전체가 멈추는 것은
 * 아닌데(고해상도 썸네일/HEIC 변환만 영향받음), {@code /actuator/health}에 올리면 기본 설정상
 * 전체 상태 집계에 포함되어 Dockerfile의 HEALTHCHECK(루트 {@code /actuator/health} 호출)가
 * 컨테이너를 unhealthy로 판정할 수 있다. 대신 게이지만 노출해 그라파나에서 추세를 보고 알림을
 * 걸 수 있게 하되, 컨테이너 재시작 여부에는 관여하지 않는다.
 *
 * @author 황제연
 */
@Configuration(proxyBeanMethods = false)
public class NativeToolMetricsConfig {

    private static final String METRIC_NAME = "native_tool_up";
    private static final String METRIC_DESCRIPTION = "네이티브 외부 프로그램이 지금 응답하는지 여부(1=정상, 0=비정상)";
    private static final String TAG_TOOL = "tool";
    static final String TOOL_VIPS = "vips";
    static final String TOOL_IMAGEMAGICK = "imagemagick";

    public NativeToolMetricsConfig(MeterRegistry registry, VipsCommand vipsCommand, ImageMagickCommand imageMagickCommand) {
        registerGauge(registry, TOOL_VIPS, vipsCommand, VipsCommand::checkAlive);
        registerGauge(registry, TOOL_IMAGEMAGICK, imageMagickCommand, ImageMagickCommand::checkAlive);
    }

    private <T> void registerGauge(MeterRegistry registry, String tool, T command, Predicate<T> aliveCheck) {
        Gauge.builder(METRIC_NAME, command, c -> aliveCheck.test(c) ? 1 : 0)
            .tag(TAG_TOOL, tool)
            .description(METRIC_DESCRIPTION)
            .strongReference(true)
            .register(registry);
    }
}
