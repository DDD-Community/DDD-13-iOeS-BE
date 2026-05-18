package com.ioes.photo.domain.myspot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.ioes.photo.domain.myspot.dto.MySpotListResponse;
import com.ioes.photo.domain.myspot.mapper.MySpotMapper;
import com.ioes.photo.domain.myspot.mapper.MySpotRow;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.global.storage.StorageService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link MySpotService} 단위 테스트.
 *
 * @author 김성민
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MySpotService 단위 테스트")
class MySpotServiceTest {

    @Mock MySpotMapper mySpotMapper;
    @Mock SpotImageRepository spotImageRepository;
    @Mock StorageService storageService;

    @InjectMocks MySpotService mySpotService;

    private static final Long USER_ID = 1L;
    private static final Long SPOT_ID = 10L;
    private static final List<String> VISIBLE_CODES = List.of(
        SpotStatus.PENDING.getCode(),
        SpotStatus.PUBLISHED.getCode()
    );

    @Nested
    @DisplayName("findMySpots()")
    class FindMySpots {

        @Test
        @DisplayName("위도/경도 모두 null이면 distanceKm 없이 정상 처리된다")
        void successWhenBothNull() {
            given(mySpotMapper.findMySpots(USER_ID, null, null, VISIBLE_CODES, 0, 6)).willReturn(List.of());
            given(mySpotMapper.countMySpots(USER_ID, VISIBLE_CODES)).willReturn(0L);

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, null, null);

            assertThat(response.spots()).isEmpty();
            assertThat(response.page()).isZero();
            assertThat(response.hasNext()).isFalse();
        }

        @Test
        @DisplayName("위도만 전달해도 distanceKm=null로 정상 처리된다")
        void succeedsWithNullDistance_whenOnlyLatProvided() {
            given(mySpotMapper.findMySpots(USER_ID, 37.5, null, VISIBLE_CODES, 0, 6)).willReturn(List.of());
            given(mySpotMapper.countMySpots(USER_ID, VISIBLE_CODES)).willReturn(0L);

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, 37.5, null);

            assertThat(response.spots()).isEmpty();
        }

        @Test
        @DisplayName("경도만 전달해도 distanceKm=null로 정상 처리된다")
        void succeedsWithNullDistance_whenOnlyLngProvided() {
            given(mySpotMapper.findMySpots(USER_ID, null, 127.0, VISIBLE_CODES, 0, 6)).willReturn(List.of());
            given(mySpotMapper.countMySpots(USER_ID, VISIBLE_CODES)).willReturn(0L);

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, null, 127.0);

            assertThat(response.spots()).isEmpty();
        }

        @Test
        @DisplayName("조회된 스팟의 imageUrl은 StorageService.getUrl 결과다")
        void imageUrlIsFromStorageService() {
            MySpotRow row = buildRow(SPOT_ID, SpotStatus.PUBLISHED.getCode());
            SpotImage image = SpotImage.create(SPOT_ID, "spots/1/image.jpg");
            given(mySpotMapper.findMySpots(USER_ID, null, null, VISIBLE_CODES, 0, 6)).willReturn(List.of(row));
            given(mySpotMapper.countMySpots(USER_ID, VISIBLE_CODES)).willReturn(1L);
            given(spotImageRepository.findAllBySpotIdIn(List.of(SPOT_ID))).willReturn(List.of(image));
            given(storageService.getUrl("spots/1/image.jpg")).willReturn("https://cdn.example.com/image.jpg");

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, null, null);

            assertThat(response.spots()).hasSize(1);
            assertThat(response.spots().get(0).imageUrl()).isEqualTo("https://cdn.example.com/image.jpg");
        }

        @Test
        @DisplayName("이미지가 없는 스팟의 imageUrl은 null이다")
        void imageUrlIsNullWhenNoImage() {
            MySpotRow row = buildRow(SPOT_ID, SpotStatus.PUBLISHED.getCode());
            given(mySpotMapper.findMySpots(USER_ID, null, null, VISIBLE_CODES, 0, 6)).willReturn(List.of(row));
            given(mySpotMapper.countMySpots(USER_ID, VISIBLE_CODES)).willReturn(1L);
            given(spotImageRepository.findAllBySpotIdIn(List.of(SPOT_ID))).willReturn(List.of());

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, null, null);

            assertThat(response.spots().get(0).imageUrl()).isNull();
        }

        @Test
        @DisplayName("PENDING 상태 스팟의 status는 enum 이름(PENDING)으로 변환되어 응답된다")
        void includesPendingSpot_withEnumName() {
            MySpotRow row = buildRow(SPOT_ID, SpotStatus.PENDING.getCode());
            given(mySpotMapper.findMySpots(USER_ID, null, null, VISIBLE_CODES, 0, 6)).willReturn(List.of(row));
            given(mySpotMapper.countMySpots(USER_ID, VISIBLE_CODES)).willReturn(1L);
            given(spotImageRepository.findAllBySpotIdIn(List.of(SPOT_ID))).willReturn(List.of());

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, null, null);

            assertThat(response.spots().get(0).status()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("PUBLISHED 상태 스팟의 status는 PUBLISHED 이름으로 변환된다")
        void includesPublishedSpot_withEnumName() {
            MySpotRow row = buildRow(SPOT_ID, SpotStatus.PUBLISHED.getCode());
            given(mySpotMapper.findMySpots(USER_ID, null, null, VISIBLE_CODES, 0, 6)).willReturn(List.of(row));
            given(mySpotMapper.countMySpots(USER_ID, VISIBLE_CODES)).willReturn(1L);
            given(spotImageRepository.findAllBySpotIdIn(List.of(SPOT_ID))).willReturn(List.of());

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, null, null);

            assertThat(response.spots().get(0).status()).isEqualTo("PUBLISHED");
        }

        @Test
        @DisplayName("전체 개수가 다음 페이지 기준 초과 시 hasNext가 true다")
        void hasNextTrue_whenMorePages() {
            given(mySpotMapper.findMySpots(USER_ID, null, null, VISIBLE_CODES, 0, 6)).willReturn(List.of());
            given(mySpotMapper.countMySpots(USER_ID, VISIBLE_CODES)).willReturn(7L);

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, null, null);

            assertThat(response.hasNext()).isTrue();
        }

        @Test
        @DisplayName("전체 개수가 다음 페이지 기준 이하면 hasNext가 false다")
        void hasNextFalse_whenLastPage() {
            given(mySpotMapper.findMySpots(USER_ID, null, null, VISIBLE_CODES, 0, 6)).willReturn(List.of());
            given(mySpotMapper.countMySpots(USER_ID, VISIBLE_CODES)).willReturn(6L);

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 0, null, null);

            assertThat(response.hasNext()).isFalse();
        }

        @Test
        @DisplayName("page=1 요청 시 offset=6으로 매퍼에 전달된다")
        void passesCorrectOffset_forSecondPage() {
            given(mySpotMapper.findMySpots(USER_ID, null, null, VISIBLE_CODES, 6, 6)).willReturn(List.of());
            given(mySpotMapper.countMySpots(USER_ID, VISIBLE_CODES)).willReturn(0L);

            MySpotListResponse response = mySpotService.findMySpots(USER_ID, 1, null, null);

            assertThat(response.page()).isEqualTo(1);
        }
    }

    private MySpotRow buildRow(Long spotId, String statusCode) {
        return new MySpotRow(spotId, "테스트스팟", "SS", 37.5, 127.0, null, LocalDateTime.now(), statusCode);
    }
}
