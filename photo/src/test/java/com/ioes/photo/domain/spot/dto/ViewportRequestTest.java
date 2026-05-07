package com.ioes.photo.domain.spot.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ViewportRequest} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("ViewportRequest 단위 테스트")
class ViewportRequestTest {

    // topLeft(37.6, 127.0)  topRight(37.6, 127.1)
    // bottomLeft(37.5, 127.0)  bottomRight(37.5, 127.1)
    private static ViewportRequest standardRect() {
        return new ViewportRequest(37.6, 127.0, 37.6, 127.1, 37.5, 127.0, 37.5, 127.1);
    }

    @Nested
    @DisplayName("minLat()")
    class MinLat {

        @Test
        @DisplayName("4개 꼭짓점 중 가장 작은 위도를 반환한다")
        void returnsSmallestLat() {
            assertThat(standardRect().minLat()).isEqualTo(37.5);
        }

        @Test
        @DisplayName("모든 꼭짓점 위도가 같으면 해당 값을 반환한다")
        void allSameLat() {
            ViewportRequest req = new ViewportRequest(37.5, 127.0, 37.5, 127.1, 37.5, 127.0, 37.5, 127.1);
            assertThat(req.minLat()).isEqualTo(37.5);
        }

        @Test
        @DisplayName("음수 위도에서도 올바른 최솟값을 반환한다")
        void negativeLatitudes() {
            ViewportRequest req = new ViewportRequest(-33.8, 151.2, -33.8, 151.3, -33.9, 151.2, -33.9, 151.3);
            assertThat(req.minLat()).isEqualTo(-33.9);
        }
    }

    @Nested
    @DisplayName("maxLat()")
    class MaxLat {

        @Test
        @DisplayName("4개 꼭짓점 중 가장 큰 위도를 반환한다")
        void returnsLargestLat() {
            assertThat(standardRect().maxLat()).isEqualTo(37.6);
        }

        @Test
        @DisplayName("음수 위도에서도 올바른 최댓값을 반환한다")
        void negativeLatitudes() {
            ViewportRequest req = new ViewportRequest(-33.8, 151.2, -33.8, 151.3, -33.9, 151.2, -33.9, 151.3);
            assertThat(req.maxLat()).isEqualTo(-33.8);
        }
    }

    @Nested
    @DisplayName("minLng()")
    class MinLng {

        @Test
        @DisplayName("4개 꼭짓점 중 가장 작은 경도를 반환한다")
        void returnsSmallestLng() {
            assertThat(standardRect().minLng()).isEqualTo(127.0);
        }

        @Test
        @DisplayName("음수 경도에서도 올바른 최솟값을 반환한다")
        void negativeLongitudes() {
            ViewportRequest req = new ViewportRequest(37.5, -74.1, 37.5, -73.9, 37.4, -74.1, 37.4, -73.9);
            assertThat(req.minLng()).isEqualTo(-74.1);
        }
    }

    @Nested
    @DisplayName("maxLng()")
    class MaxLng {

        @Test
        @DisplayName("4개 꼭짓점 중 가장 큰 경도를 반환한다")
        void returnsLargestLng() {
            assertThat(standardRect().maxLng()).isEqualTo(127.1);
        }

        @Test
        @DisplayName("음수 경도에서도 올바른 최댓값을 반환한다")
        void negativeLongitudes() {
            ViewportRequest req = new ViewportRequest(37.5, -74.1, 37.5, -73.9, 37.4, -74.1, 37.4, -73.9);
            assertThat(req.maxLng()).isEqualTo(-73.9);
        }
    }

    @Nested
    @DisplayName("min/max 일관성")
    class Consistency {

        @Test
        @DisplayName("모든 꼭짓점이 같으면 min과 max가 동일하다")
        void allSameCoordinates() {
            ViewportRequest req = new ViewportRequest(37.5, 127.0, 37.5, 127.0, 37.5, 127.0, 37.5, 127.0);
            assertThat(req.minLat()).isEqualTo(req.maxLat());
            assertThat(req.minLng()).isEqualTo(req.maxLng());
        }

        @Test
        @DisplayName("minLat은 항상 maxLat보다 작거나 같다")
        void minAlwaysLessThanOrEqualToMax() {
            assertThat(standardRect().minLat()).isLessThanOrEqualTo(standardRect().maxLat());
            assertThat(standardRect().minLng()).isLessThanOrEqualTo(standardRect().maxLng());
        }
    }
}
