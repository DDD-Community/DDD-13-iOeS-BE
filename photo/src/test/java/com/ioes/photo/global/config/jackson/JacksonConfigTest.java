package com.ioes.photo.global.config.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link JacksonConfig} ObjectMapper 설정 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("JacksonConfig - ObjectMapper 설정 테스트")
class JacksonConfigTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new JacksonConfig().objectMapper();
    }

    @Test
    @DisplayName("null이 아닌 필드는 JSON 직렬화 결과에 포함된다")
    void serialize_includesNonNullFields() throws JsonProcessingException {
        SampleDto dto = new SampleDto("hello", "world");

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"name\":\"hello\"");
        assertThat(json).contains("\"optionalField\":\"world\"");
    }

    @Test
    @DisplayName("LocalDateTime이 ISO-8601 문자열로 직렬화된다 (타임스탬프 배열 아님)")
    void serialize_localDateTimeAsIsoString() throws JsonProcessingException {
        DateDto dto = new DateDto(LocalDateTime.of(2024, 6, 15, 10, 30, 0));

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("2024-06-15T10:30:00");
        assertThat(json).doesNotContain("[2024,6,15");
    }

    @Test
    @DisplayName("역직렬화 시 알 수 없는 프로퍼티가 있어도 예외가 발생하지 않는다")
    void deserialize_ignoresUnknownProperties() {
        String json = "{\"name\":\"test\",\"unknownField\":\"unexpected value\"}";

        assertThatCode(() -> objectMapper.readValue(json, SampleDto.class))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("JSON 직렬화 후 역직렬화 시 원본 객체와 동일하다 (round-trip)")
    void json_roundTrip() throws JsonProcessingException {
        SampleDto original = new SampleDto("hello", "world");

        String json = objectMapper.writeValueAsString(original);
        SampleDto result = objectMapper.readValue(json, SampleDto.class);

        assertThat(result.getName()).isEqualTo(original.getName());
        assertThat(result.getOptionalField()).isEqualTo(original.getOptionalField());
    }

    @Test
    @DisplayName("클래스 레벨 @JsonInclude(NON_NULL)이 적용된 객체도 null 필드를 제외한다")
    void serialize_classLevelJsonIncludeExcludesNull() throws JsonProcessingException {
        AlwaysNullDto dto = new AlwaysNullDto(null);

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).doesNotContain("alwaysNull");
    }

    // ---- 내부 테스트용 DTO ----

    static class SampleDto {
        private final String name;
        private final String optionalField;

        SampleDto() { this(null, null); }

        SampleDto(String name, String optionalField) {
            this.name = name;
            this.optionalField = optionalField;
        }

        public String getName() { return name; }
        public String getOptionalField() { return optionalField; }
    }

    static class DateDto {
        private final LocalDateTime createdAt;

        DateDto() { this(null); }

        DateDto(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class AlwaysNullDto {
        private final String alwaysNull;

        AlwaysNullDto() { this(null); }

        AlwaysNullDto(String alwaysNull) { this.alwaysNull = alwaysNull; }

        public String getAlwaysNull() { return alwaysNull; }
    }
}