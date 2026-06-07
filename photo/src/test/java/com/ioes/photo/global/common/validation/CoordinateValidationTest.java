package com.ioes.photo.global.common.validation;

import com.ioes.photo.domain.spot.dto.ViewportRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Latitude}, {@link Longitude} 유효성 검증 어노테이션 테스트.
 *
 * @author 황제연
 */
@DisplayName("좌표 유효성 검증 단위 테스트")
class CoordinateValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // ── @Latitude ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("@Latitude - 범위 검증")
    class LatitudeRange {

        @Test
        @DisplayName("유효한 위도(37.123456)는 통과한다")
        void passesForValidLatitude() {
            Set<ConstraintViolation<ViewportRequest>> violations = validate(
                new ViewportRequest(37.123456, 127.0, 37.123456, 127.1, 37.0, 127.0, 37.0, 127.1)
            );
            assertThat(latViolations(violations)).isEmpty();
        }

        @Test
        @DisplayName("위도 최솟값(-90.0)은 통과한다")
        void passesForMinLatitude() {
            assertThat(latViolations(validate(
                new ViewportRequest(-90.0, 127.0, -90.0, 127.1, -90.0, 127.0, -90.0, 127.1)
            ))).isEmpty();
        }

        @Test
        @DisplayName("위도 최댓값(90.0)은 통과한다")
        void passesForMaxLatitude() {
            assertThat(latViolations(validate(
                new ViewportRequest(90.0, 127.0, 90.0, 127.1, 90.0, 127.0, 90.0, 127.1)
            ))).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(doubles = {-90.000001, -91.0, -180.0})
        @DisplayName("위도 하한(-90.0)을 벗어나면 실패한다")
        void failsBelowMinLatitude(double invalidLat) {
            assertThat(latViolations(validate(
                new ViewportRequest(invalidLat, 127.0, 37.0, 127.1, 37.0, 127.0, 37.0, 127.1)
            ))).isNotEmpty();
        }

        @ParameterizedTest
        @ValueSource(doubles = {90.000001, 91.0, 180.0})
        @DisplayName("위도 상한(90.0)을 벗어나면 실패한다")
        void failsAboveMaxLatitude(double invalidLat) {
            assertThat(latViolations(validate(
                new ViewportRequest(invalidLat, 127.0, 37.0, 127.1, 37.0, 127.0, 37.0, 127.1)
            ))).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("@Latitude - 소수점 자릿수 검증")
    class LatitudePrecision {

        @Test
        @DisplayName("소수점 6자리 위도는 통과한다")
        void passesForSixDecimalPlaces() {
            assertThat(latViolations(validate(
                new ViewportRequest(37.123456, 127.0, 37.0, 127.0, 37.0, 127.0, 37.0, 127.0)
            ))).isEmpty();
        }

        @Test
        @DisplayName("소수점 7자리 위도는 실패한다")
        void failsForSevenDecimalPlaces() {
            assertThat(latViolations(validate(
                new ViewportRequest(37.1234567, 127.0, 37.0, 127.0, 37.0, 127.0, 37.0, 127.0)
            ))).isNotEmpty();
        }

        @Test
        @DisplayName("정수 위도는 통과한다")
        void passesForIntegerLatitude() {
            assertThat(latViolations(validate(
                new ViewportRequest(37.0, 127.0, 37.0, 127.0, 37.0, 127.0, 37.0, 127.0)
            ))).isEmpty();
        }
    }

    // ── @Longitude ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("@Longitude - 범위 검증")
    class LongitudeRange {

        @Test
        @DisplayName("유효한 경도(127.123456)는 통과한다")
        void passesForValidLongitude() {
            assertThat(lngViolations(validate(
                new ViewportRequest(37.0, 127.123456, 37.0, 127.123456, 37.0, 127.123456, 37.0, 127.123456)
            ))).isEmpty();
        }

        @Test
        @DisplayName("경도 최솟값(-180.0)은 통과한다")
        void passesForMinLongitude() {
            assertThat(lngViolations(validate(
                new ViewportRequest(37.0, -180.0, 37.0, -180.0, 37.0, -180.0, 37.0, -180.0)
            ))).isEmpty();
        }

        @Test
        @DisplayName("경도 최댓값(180.0)은 통과한다")
        void passesForMaxLongitude() {
            assertThat(lngViolations(validate(
                new ViewportRequest(37.0, 180.0, 37.0, 180.0, 37.0, 180.0, 37.0, 180.0)
            ))).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(doubles = {-180.000001, -181.0, -360.0})
        @DisplayName("경도 하한(-180.0)을 벗어나면 실패한다")
        void failsBelowMinLongitude(double invalidLng) {
            assertThat(lngViolations(validate(
                new ViewportRequest(37.0, invalidLng, 37.0, 127.0, 37.0, 127.0, 37.0, 127.0)
            ))).isNotEmpty();
        }

        @ParameterizedTest
        @ValueSource(doubles = {180.000001, 181.0, 360.0})
        @DisplayName("경도 상한(180.0)을 벗어나면 실패한다")
        void failsAboveMaxLongitude(double invalidLng) {
            assertThat(lngViolations(validate(
                new ViewportRequest(37.0, invalidLng, 37.0, 127.0, 37.0, 127.0, 37.0, 127.0)
            ))).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("@Longitude - 소수점 자릿수 검증")
    class LongitudePrecision {

        @Test
        @DisplayName("소수점 6자리 경도는 통과한다")
        void passesForSixDecimalPlaces() {
            assertThat(lngViolations(validate(
                new ViewportRequest(37.0, 127.123456, 37.0, 127.0, 37.0, 127.0, 37.0, 127.0)
            ))).isEmpty();
        }

        @Test
        @DisplayName("소수점 7자리 경도는 실패한다")
        void failsForSevenDecimalPlaces() {
            assertThat(lngViolations(validate(
                new ViewportRequest(37.0, 127.1234567, 37.0, 127.0, 37.0, 127.0, 37.0, 127.0)
            ))).isNotEmpty();
        }

        @Test
        @DisplayName("정수 경도는 통과한다")
        void passesForIntegerLongitude() {
            assertThat(lngViolations(validate(
                new ViewportRequest(37.0, 127.0, 37.0, 127.0, 37.0, 127.0, 37.0, 127.0)
            ))).isEmpty();
        }
    }

    // ── helper ──────────────────────────────────────────────────────────────

    private Set<ConstraintViolation<ViewportRequest>> validate(ViewportRequest request) {
        return validator.validate(request);
    }

    private static Set<ConstraintViolation<ViewportRequest>> latViolations(
        Set<ConstraintViolation<ViewportRequest>> all
    ) {
        return all.stream()
            .filter(v -> v.getPropertyPath().toString().toLowerCase().contains("lat"))
            .collect(java.util.stream.Collectors.toSet());
    }

    private static Set<ConstraintViolation<ViewportRequest>> lngViolations(
        Set<ConstraintViolation<ViewportRequest>> all
    ) {
        return all.stream()
            .filter(v -> v.getPropertyPath().toString().toLowerCase().contains("lng"))
            .collect(java.util.stream.Collectors.toSet());
    }
}
