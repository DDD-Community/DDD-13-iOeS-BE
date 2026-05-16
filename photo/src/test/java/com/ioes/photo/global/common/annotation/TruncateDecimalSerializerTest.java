package com.ioes.photo.global.common.annotation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TruncateDecimal} 애노테이션 및 {@link TruncateDecimalSerializer} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("TruncateDecimal 애노테이션 단위 테스트")
class TruncateDecimalSerializerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    record TestDto(@TruncateDecimal Double value) {}
    record TestDtoScale2(@TruncateDecimal(scale = 2) Double value) {}
    record TestDtoNoAnnotation(Double value) {}

    // ── 소수점 1자리 절삭 ──────────────────────────────────────────────────

    @Nested
    @DisplayName("소수점 1자리 절삭 (기본값)")
    class DefaultScale {

        @Test
        @DisplayName("소수점 2자리 이상이면 1자리로 절삭한다")
        void shouldTruncateToOneDecimal() throws Exception {
            String json = objectMapper.writeValueAsString(new TestDto(1.29));
            assertThat(json).isEqualTo("{\"value\":1.2}");
        }

        @Test
        @DisplayName("절삭이므로 반올림하지 않는다 (1.25 → 1.2)")
        void shouldTruncateNotRound() throws Exception {
            String json = objectMapper.writeValueAsString(new TestDto(1.25));
            assertThat(json).isEqualTo("{\"value\":1.2}");
        }

        @Test
        @DisplayName("1.99는 1.9로 절삭된다")
        void shouldTruncate_1_99() throws Exception {
            String json = objectMapper.writeValueAsString(new TestDto(1.99));
            assertThat(json).isEqualTo("{\"value\":1.9}");
        }

        @Test
        @DisplayName("이미 소수점 1자리면 그대로 유지된다")
        void shouldKeepIfAlreadyOneDecimal() throws Exception {
            String json = objectMapper.writeValueAsString(new TestDto(3.5));
            assertThat(json).isEqualTo("{\"value\":3.5}");
        }

        @Test
        @DisplayName("0.0은 0.0으로 직렬화된다")
        void shouldSerializeZero() throws Exception {
            String json = objectMapper.writeValueAsString(new TestDto(0.0));
            assertThat(json).isEqualTo("{\"value\":0.0}");
        }

        @Test
        @DisplayName("null은 null로 직렬화된다")
        void shouldSerializeNull() throws Exception {
            String json = objectMapper.writeValueAsString(new TestDto(null));
            assertThat(json).isEqualTo("{\"value\":null}");
        }
    }

    // ── scale 커스텀 ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("scale = 2 커스텀")
    class CustomScale {

        @Test
        @DisplayName("scale=2이면 소수점 3자리를 2자리로 절삭한다")
        void shouldTruncateToTwoDecimals() throws Exception {
            String json = objectMapper.writeValueAsString(new TestDtoScale2(1.299));
            assertThat(json).isEqualTo("{\"value\":1.29}");
        }

        @Test
        @DisplayName("scale=2이면 1.295 → 1.29로 절삭한다")
        void shouldTruncateNotRound_scale2() throws Exception {
            String json = objectMapper.writeValueAsString(new TestDtoScale2(1.295));
            assertThat(json).isEqualTo("{\"value\":1.29}");
        }
    }

    // ── 역직렬화(요청) 미적용 검증 ──────────────────────────────────────

    @Nested
    @DisplayName("역직렬화(요청 파싱) 미적용")
    class Deserialization {

        @Test
        @DisplayName("JSON 파싱 시 원본 값을 절삭 없이 그대로 유지한다")
        void shouldNotTruncate_whenDeserializing() throws Exception {
            TestDto dto = objectMapper.readValue("{\"value\":1.29}", TestDto.class);
            assertThat(dto.value()).isEqualTo(1.29);
        }

        @Test
        @DisplayName("소수점이 긴 값도 역직렬화 시 절삭되지 않는다")
        void shouldPreserveFullPrecision_whenDeserializing() throws Exception {
            TestDto dto = objectMapper.readValue("{\"value\":1.23456789}", TestDto.class);
            assertThat(dto.value()).isEqualTo(1.23456789);
        }
    }

    // ── 애노테이션 미적용 시 ──────────────────────────────────────────────

    @Nested
    @DisplayName("애노테이션 미적용 필드")
    class NoAnnotation {

        @Test
        @DisplayName("애노테이션이 없으면 원래 값 그대로 직렬화된다")
        void shouldSerializeOriginalValue_whenNoAnnotation() throws Exception {
            String json = objectMapper.writeValueAsString(new TestDtoNoAnnotation(1.29));
            assertThat(json).contains("1.29");
        }
    }
}
