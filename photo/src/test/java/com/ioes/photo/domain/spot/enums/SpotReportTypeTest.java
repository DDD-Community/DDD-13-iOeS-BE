package com.ioes.photo.domain.spot.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SpotReportType} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("SpotReportType 단위 테스트")
class SpotReportTypeTest {

    @Test
    @DisplayName("CodedEnum 인터페이스를 구현한다")
    void implementsCodedEnum() {
        assertThat(SpotReportType.LOCATION_ERROR).isInstanceOf(CodedEnum.class);
    }

    @ParameterizedTest(name = "{0} → code={1}, description={2}")
    @CsvSource({
        "LOCATION_ERROR, LO, 위치 오류",
        "WRONG_NAME,     WN, 잘못된 이름",
        "ETC,            ET, 기타"
    })
    @DisplayName("각 값의 code와 description이 올바르다")
    void codeAndDescriptionAreCorrect(String name, String expectedCode, String expectedDescription) {
        SpotReportType type = SpotReportType.valueOf(name.trim());

        assertThat(type.getCode()).isEqualTo(expectedCode.trim());
        assertThat(type.getDescription()).isEqualTo(expectedDescription.trim());
    }

    @Test
    @DisplayName("code 값이 모두 고유하다")
    void allCodesAreUnique() {
        long distinctCount = java.util.Arrays.stream(SpotReportType.values())
            .map(SpotReportType::getCode)
            .distinct()
            .count();

        assertThat(distinctCount).isEqualTo(SpotReportType.values().length);
    }

    @Test
    @DisplayName("총 3개 값이 정의되어 있다")
    void hasThreeValues() {
        assertThat(SpotReportType.values()).hasSize(3);
    }
}
