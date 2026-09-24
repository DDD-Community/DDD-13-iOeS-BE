package com.ioes.photo.global.config.web;

import com.ioes.photo.PhotoApplication;
import com.ioes.photo.global.auth.CurrentUserIdArgumentResolver;
import com.ioes.photo.global.config.web.converter.CodedEnumConverterRegistrar;
import com.ioes.photo.global.config.web.properties.CorsProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 전역 설정.
 *
 * @see CorsProperties
 * @author 황제연
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserIdArgumentResolver currentUserIdArgumentResolver;
    private final CorsProperties corsProperties;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserIdArgumentResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new))
            .allowedMethods(corsProperties.allowedMethods().toArray(String[]::new))
            .allowedHeaders("*")
            .maxAge(corsProperties.maxAge());
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        new CodedEnumConverterRegistrar().registerAll(registry, PhotoApplication.class.getPackageName());
    }
}
