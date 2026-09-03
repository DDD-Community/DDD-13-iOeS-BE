package com.ioes.photo.global.config.web.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ioes.photo.domain.spot.enums.SpotTheme;
import fixture.codedenum.AmbiguousEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link StringToCodedEnumConverter} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("StringToCodedEnumConverter 테스트")
class StringToCodedEnumConverterTest {

    private final StringToCodedEnumConverter<SpotTheme> converter =
        new StringToCodedEnumConverter<>(SpotTheme.class);

    @Nested
    @DisplayName("변환")
    class Convert {

        @Test
        @DisplayName("code로 매칭된다")
        void matchesByCode() {
            assertThat(converter.convert("SS")).isEqualTo(SpotTheme.SUNSET);
        }

        @Test
        @DisplayName("code 매칭이 없으면 enum 이름으로 폴백된다")
        void fallsBackToName() {
            assertThat(converter.convert("SUNSET")).isEqualTo(SpotTheme.SUNSET);
        }

        @Test
        @DisplayName("대소문자는 구분한다")
        void isCaseSensitive() {
            assertThatThrownBy(() -> converter.convert("ss"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("앞뒤 공백은 trim 후 매칭된다")
        void trimsWhitespace() {
            assertThat(converter.convert(" SS ")).isEqualTo(SpotTheme.SUNSET);
            assertThat(converter.convert(" SUNSET ")).isEqualTo(SpotTheme.SUNSET);
        }

        @Test
        @DisplayName("code와 이름 어디에도 매칭되지 않으면 IllegalArgumentException")
        void throwsWhenNoMatch() {
            assertThatThrownBy(() -> converter.convert("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("빈 문자열은 null로 변환된다 (필터 미선택 쿼리 파라미터 호환)")
        void convertsEmptyStringToNull() {
            assertThat(converter.convert("")).isNull();
        }

        @Test
        @DisplayName("공백뿐인 문자열도 null로 변환된다")
        void convertsBlankStringToNull() {
            assertThat(converter.convert("   ")).isNull();
        }
    }

    @Nested
    @DisplayName("모호성 검증 (생성자 fail-fast)")
    class AmbiguityValidation {

        @Test
        @DisplayName("한 상수의 name이 다른 상수의 code와 겹치면 생성 시점에 예외가 발생한다")
        void throwsOnAmbiguousEnum() {
            assertThatThrownBy(() -> new StringToCodedEnumConverter<>(AmbiguousEnum.class))
                .isInstanceOf(IllegalStateException.class);
        }
    }
}
