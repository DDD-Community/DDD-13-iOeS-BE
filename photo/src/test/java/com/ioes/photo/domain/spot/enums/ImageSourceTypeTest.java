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
 * {@link ImageSourceType} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("ImageSourceType 단위 테스트")
class ImageSourceTypeTest {

    static Stream<ImageSourceType> types() {
        return Arrays.stream(ImageSourceType.values());
    }

    @Test
    @DisplayName("모든 값의 코드는 서로 중복되지 않는다")
    void codesAreUnique() {
        assertThat(Arrays.stream(ImageSourceType.values()).map(ImageSourceType::getCode))
            .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("image_source_type 컬럼 길이(1자) 안에 들어간다")
    void codesFitColumnLength() {
        assertThat(Arrays.stream(ImageSourceType.values()).map(ImageSourceType::getCode))
            .allSatisfy(code -> assertThat(code).isNotBlank().hasSize(1));
    }

    @Test
    @DisplayName("기존에 적재된 코드는 변경되지 않는다")
    void legacyCodesAreStable() {
        assertThat(ImageSourceType.INTERNAL.getCode()).isEqualTo("I");
        assertThat(ImageSourceType.EXTERNAL.getCode()).isEqualTo("E");
    }

    @Nested
    @DisplayName("fromCode")
    class FromCode {

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.ioes.photo.domain.spot.enums.ImageSourceTypeTest#types")
        @DisplayName("코드로 원래 값을 복원한다")
        void roundTrip(ImageSourceType type) {
            assertThat(ImageSourceType.fromCode(type.getCode())).isEqualTo(type);
        }

        @Test
        @DisplayName("알 수 없는 코드는 예외가 발생한다")
        void unknownCodeThrows() {
            assertThatThrownBy(() -> ImageSourceType.fromCode("Z"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Z");
        }

        @Test
        @DisplayName("null 코드는 예외가 발생한다")
        void nullCodeThrows() {
            assertThatThrownBy(() -> ImageSourceType.fromCode(null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
