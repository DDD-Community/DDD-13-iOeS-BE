package com.ioes.photo.global.config.jackson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson ObjectMapper 설정 클래스.
 *
 * JSON / XML 동시 지원<
 * jackson-dataformat-xml이 클래스패스에 있으면 Spring Boot가 자동으로
 * JSON과 XML 두 가지 메시지 컨버터를 모두 등록합니다.
 * JSON: MappingJackson2HttpMessageConverter - application/json
 * XML: MappingJackson2XmlHttpMessageConverter - application/xml
 *
 *
 * JAXB 어노테이션
 * 모델 클래스에 @XmlRootElement, @XmlElement 등 Jakarta XML Bind 어노테이션을 사용할 수 있습니다.
 *
 *
 * @author 황제연
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
