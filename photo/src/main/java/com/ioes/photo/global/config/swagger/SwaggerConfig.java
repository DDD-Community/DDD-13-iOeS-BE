package com.ioes.photo.global.config.swagger;

import com.ioes.photo.global.auth.CurrentUserId;
import com.ioes.photo.global.config.swagger.properties.SwaggerProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Arrays;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.customizers.OperationCustomizer;
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

    @Bean
    public OperationCustomizer hideCurrentUserIdParameter() {
        return (operation, handlerMethod) -> {
            Arrays.stream(handlerMethod.getMethodParameters())
                .filter(p -> p.hasParameterAnnotation(CurrentUserId.class))
                .map(p -> Objects.requireNonNullElse(p.getParameterName(), "userId"))
                .forEach(name -> {
                    if (operation.getParameters() != null) {
                        operation.getParameters().removeIf(p -> name.equals(p.getName()));
                    }
                });
            return operation;
        };
    }

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
