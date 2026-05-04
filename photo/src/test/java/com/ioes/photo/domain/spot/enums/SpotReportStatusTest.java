package com.ioes.photo.domain.spot.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SpotReportStatus} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("SpotReportStatus 단위 테스트")
class SpotReportStatusTest {

    @Test
    @DisplayName("CodedEnum 인터페이스를 구현한다")
    void implementsCodedEnum() {
        assertThat(SpotReportStatus.PENDING).isInstanceOf(CodedEnum.class);
    }

    @ParameterizedTest(name = "{0} → code={1}, description={2}")
    @CsvSource({
        "PENDING,  P, 대기중",
        "RESOLVED, R, 처리완료"
    })
    @DisplayName("각 값의 code와 description이 올바르다")
    void codeAndDescriptionAreCorrect(String name, String expectedCode, String expectedDescription) {
        SpotReportStatus status = SpotReportStatus.valueOf(name.trim());

        assertThat(status.getCode()).isEqualTo(expectedCode.trim());
        assertThat(status.getDescription()).isEqualTo(expectedDescription.trim());
    }

    @Test
    @DisplayName("code 값이 모두 고유하다")
    void allCodesAreUnique() {
        long distinctCount = java.util.Arrays.stream(SpotReportStatus.values())
            .map(SpotReportStatus::getCode)
            .distinct()
            .count();

        assertThat(distinctCount).isEqualTo(SpotReportStatus.values().length);
    }

    @Test
    @DisplayName("총 2개 값이 정의되어 있다")
    void hasTwoValues() {
        assertThat(SpotReportStatus.values()).hasSize(2);
    }
}
