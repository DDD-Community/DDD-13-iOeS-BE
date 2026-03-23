package com.ioes.photo.global.config.swagger;

import com.ioes.photo.global.config.swagger.properties.SwaggerProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI(Swagger) 문서 설정 클래스.
 *
 * @see SwaggerProperties
 * @author 황제연
 */
@Configuration
@RequiredArgsConstructor
public class SwaggerConfig {

    private final SwaggerProperties swaggerProperties;

    /**
     * JWT Bearer 인증 보안 스키마가 포함된 빈을 생성합니다
     *
     * @return 설정된 OpenAPI 인스턴스
     */
    @Bean
    public OpenAPI openAPI() {
        String schemeName = swaggerProperties.securitySchemeName();
        return new OpenAPI()
            .info(new Info()
                .title(swaggerProperties.title())
                .version(swaggerProperties.version())
                .description(swaggerProperties.description()))
            .addSecurityItem(new SecurityRequirement().addList(schemeName))
            .components(new Components()
                .addSecuritySchemes(schemeName, new SecurityScheme()
                    .name(schemeName)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
