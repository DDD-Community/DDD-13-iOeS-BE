package com.ioes.photo.domain.share.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("SpotIdCoder 테스트")
class SpotIdCoderTest {

    @Nested
    @DisplayName("앱 제공 테스트 벡터")
    class TestVectors {

        @ParameterizedTest(name = "{1} → spotId {0}")
        @CsvSource({
            "0, k-5",
            "1, k-k",
            "61, k-z",
            "62, k-k5",
            "123, k-kz",
            "1000, k-dL",
            "45678, k-81a",
            "9007199254740991, k-tWhIY49fp"
        })
        @DisplayName("토큰을 spotId로 디코딩한다")
        void decodesToken(long spotId, String token) {
            assertThat(SpotIdCoder.decodeSpotId(token)).contains(spotId);
        }
    }

    @Nested
    @DisplayName("디코딩 실패 케이스")
    class Failures {

        @ParameterizedTest(name = "{0} → 실패")
        @ValueSource(strings = {
            "kz",             // 하이픈 없음
            "R-kz",           // type=2 (미지원)
            "k-a!b",          // 알파벳 외 문자
            "k-zzzzzzzzzzz"   // int64 오버플로
        })
        @DisplayName("잘못된 토큰은 빈 결과를 반환한다")
        void returnsEmptyForInvalidToken(String token) {
            assertThat(SpotIdCoder.decodeSpotId(token)).isEmpty();
        }

        @Test
        @DisplayName("null 토큰은 빈 결과를 반환한다")
        void returnsEmptyForNull() {
            assertThat(SpotIdCoder.decodeSpotId(null)).isEmpty();
        }
    }

    @Test
    @DisplayName("빈 id는 iOS 규칙대로 0으로 디코딩된다")
    void decodesEmptyIdToZero() {
        assertThat(SpotIdCoder.decodeSpotId("k-")).contains(0L);
    }

    @Test
    @DisplayName("인코딩/디코딩이 왕복 일치한다")
    void roundTrips() {
        Optional<Long> decoded = SpotIdCoder.decodeSpotId("k-kz");
        assertThat(decoded).contains(123L);
    }
}
