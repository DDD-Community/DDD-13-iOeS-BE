package com.ioes.photo.domain.spot.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link SpotTheme} 단위 테스트.
 *
 * 테마는 계속 추가되는 값이므로, 코드/이름이 겹쳐 기존 데이터를 오염시키는 회귀를 막는 데 초점을 둔다.
 *
 * @author 황제연
 */
@DisplayName("SpotTheme 단위 테스트")
class SpotThemeTest {

    static Stream<SpotTheme> themes() {
        return Arrays.stream(SpotTheme.values());
    }

    @Nested
    @DisplayName("코드 정의")
    class CodeDefinition {

        @Test
        @DisplayName("모든 테마의 코드는 서로 중복되지 않는다")
        void codesAreUnique() {
            assertThat(Arrays.stream(SpotTheme.values()).map(SpotTheme::getCode))
                .doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("모든 테마의 이름은 서로 중복되지 않는다")
        void labelsAreUnique() {
            assertThat(Arrays.stream(SpotTheme.values()).map(SpotTheme::getLabel))
                .doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("코드는 theme 컬럼 길이(4자) 안에 들어간다")
        void codesFitColumnLength() {
            assertThat(Arrays.stream(SpotTheme.values()).map(SpotTheme::getCode))
                .allSatisfy(code -> assertThat(code).isNotBlank().hasSizeLessThanOrEqualTo(4));
        }

        @Test
        @DisplayName("기존에 적재된 노을/윤슬 코드는 변경되지 않는다")
        void legacyCodesAreStable() {
            assertThat(SpotTheme.SUNSET.getCode()).isEqualTo("SS");
            assertThat(SpotTheme.YUNSEUL.getCode()).isEqualTo("YS");
        }

        @Test
        @DisplayName("햇살/야경 테마가 추가되어 있다")
        void newThemesArePresent() {
            assertThat(SpotTheme.SUNLIGHT.getLabel()).isEqualTo("햇살");
            assertThat(SpotTheme.NIGHT_VIEW.getLabel()).isEqualTo("야경");
        }
    }

    @Nested
    @DisplayName("fromCode")
    class FromCode {

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.ioes.photo.domain.spot.enums.SpotThemeTest#themes")
        @DisplayName("코드로 원래 테마를 복원한다")
        void roundTrip(SpotTheme theme) {
            assertThat(SpotTheme.fromCode(theme.getCode())).isEqualTo(theme);
        }

        @Test
        @DisplayName("알 수 없는 코드는 예외가 발생한다")
        void unknownCodeThrows() {
            assertThatThrownBy(() -> SpotTheme.fromCode("ZZ"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ZZ");
        }

        @Test
        @DisplayName("null 코드는 예외가 발생한다")
        void nullCodeThrows() {
            assertThatThrownBy(() -> SpotTheme.fromCode(null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("fromLabel")
    class FromLabel {

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.ioes.photo.domain.spot.enums.SpotThemeTest#themes")
        @DisplayName("이름으로 원래 테마를 복원한다")
        void roundTrip(SpotTheme theme) {
            assertThat(SpotTheme.fromLabel(theme.getLabel())).isEqualTo(theme);
        }

        @Test
        @DisplayName("알 수 없는 이름은 예외가 발생한다")
        void unknownLabelThrows() {
            assertThatThrownBy(() -> SpotTheme.fromLabel("무지개"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("무지개");
        }
    }

    @Test
    @DisplayName("labelGuide는 모든 테마를 이름과 함께 나열한다")
    void labelGuideListsEveryTheme() {
        String guide = SpotTheme.labelGuide();

        assertThat(guide).contains("SUNSET=노을", "YUNSEUL=윤슬", "SUNLIGHT=햇살", "NIGHT_VIEW=야경");
    }
}
