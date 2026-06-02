package com.ioes.photo.domain.spot.service;

import com.ioes.photo.domain.crowdarea.service.CrowdAreaMapper;
import com.ioes.photo.domain.spot.dto.SpotAdminCreateRequest;
import com.ioes.photo.domain.spot.dto.SpotAdminCreateRequest.Item;
import com.ioes.photo.domain.spot.dto.SpotAdminCreateResponse;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * {@link SpotAdminService} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpotAdminService 단위 테스트")
@SuppressWarnings("unchecked")
class SpotAdminServiceTest {

    @Mock SpotRepository spotRepository;
    @Mock CrowdAreaMapper crowdAreaMapper;

    @InjectMocks SpotAdminService spotAdminService;

    // ── createSpots ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("createSpots()")
    class CreateSpots {

        @Test
        @DisplayName("스팟 목록을 저장하고 ID와 이름을 반환한다")
        void shouldReturnCreatedSpots() {
            Item item = buildItem("한강 노을 포인트", SpotTheme.SUNSET, 37.55, 126.99);
            SpotAdminCreateRequest request = new SpotAdminCreateRequest(List.of(item));

            Spot savedSpot = buildSavedSpot(1L, "한강 노을 포인트");
            given(spotRepository.saveAll(anyList())).willReturn(List.of(savedSpot));

            SpotAdminCreateResponse response = spotAdminService.createSpots(request);

            assertThat(response.count()).isEqualTo(1);
            assertThat(response.created()).hasSize(1);
            assertThat(response.created().get(0).spotId()).isEqualTo(1L);
            assertThat(response.created().get(0).name()).isEqualTo("한강 노을 포인트");
        }

        @Test
        @DisplayName("여러 스팟을 배치로 등록할 수 있다")
        void shouldHandleBatch() {
            List<Item> items = List.of(
                buildItem("스팟A", SpotTheme.SUNSET, 37.55, 126.99),
                buildItem("스팟B", SpotTheme.YUNSEUL, 37.56, 127.00)
            );
            SpotAdminCreateRequest request = new SpotAdminCreateRequest(items);

            Spot spot1 = buildSavedSpot(1L, "스팟A");
            Spot spot2 = buildSavedSpot(2L, "스팟B");
            given(spotRepository.saveAll(anyList())).willReturn(List.of(spot1, spot2));

            SpotAdminCreateResponse response = spotAdminService.createSpots(request);

            assertThat(response.count()).isEqualTo(2);
            assertThat(response.created()).extracting("name").containsExactly("스팟A", "스팟B");
        }

        @Test
        @DisplayName("등록된 스팟은 PUBLISHED 상태이다")
        void shouldCreateWithPublishedStatus() {
            Item item = buildItem("한강 노을 포인트", SpotTheme.SUNSET, 37.55, 126.99);
            SpotAdminCreateRequest request = new SpotAdminCreateRequest(List.of(item));
            given(spotRepository.saveAll(anyList())).willReturn(List.of(buildSavedSpot(1L, "한강 노을 포인트")));

            spotAdminService.createSpots(request);

            ArgumentCaptor<List<Spot>> captor = ArgumentCaptor.forClass(List.class);
            then(spotRepository).should().saveAll(captor.capture());
            assertThat(captor.getValue()).allSatisfy(spot ->
                assertThat(spot.getStatus()).isEqualTo(SpotStatus.PUBLISHED)
            );
        }

        @Test
        @DisplayName("location(PostGIS geometry)이 위도/경도로 자동 생성된다")
        void shouldAutoGenerateLocation() {
            Item item = buildItem("한강 노을 포인트", SpotTheme.SUNSET, 37.55, 126.99);
            SpotAdminCreateRequest request = new SpotAdminCreateRequest(List.of(item));
            given(spotRepository.saveAll(anyList())).willReturn(List.of(buildSavedSpot(1L, "한강 노을 포인트")));

            spotAdminService.createSpots(request);

            ArgumentCaptor<List<Spot>> captor = ArgumentCaptor.forClass(List.class);
            then(spotRepository).should().saveAll(captor.capture());
            Spot spot = captor.getValue().get(0);
            assertThat(spot.getLocation()).isNotNull();
            assertThat(spot.getLocation().getX()).isEqualTo(126.99); // X = longitude
            assertThat(spot.getLocation().getY()).isEqualTo(37.55);  // Y = latitude
        }

        @Test
        @DisplayName("선택 필드(gridNx, gridNy, crowdAreaName)는 null이어도 저장된다")
        void shouldSaveWithNullOptionalFields() {
            Item item = new Item("스팟", null, SpotTheme.SUNSET, 37.5, 127.0, null, null, null, null);
            SpotAdminCreateRequest request = new SpotAdminCreateRequest(List.of(item));
            given(spotRepository.saveAll(anyList())).willReturn(List.of(buildSavedSpot(1L, "스팟")));

            spotAdminService.createSpots(request);

            ArgumentCaptor<List<Spot>> captor = ArgumentCaptor.forClass(List.class);
            then(spotRepository).should().saveAll(captor.capture());
            Spot spot = captor.getValue().get(0);
            assertThat(spot.getGridNx()).isNull();
            assertThat(spot.getGridNy()).isNull();
            assertThat(spot.getCrowdAreaName()).isNull();
        }

        @Test
        @DisplayName("comment, address 등 선택 문자열 필드도 저장된다")
        void shouldSaveOptionalStringFields() {
            Item item = new Item("스팟", "설명입니다", SpotTheme.SUNSET, 37.5, 127.0, "서울시 마포구", 60, 126, "한강공원");
            SpotAdminCreateRequest request = new SpotAdminCreateRequest(List.of(item));
            given(spotRepository.saveAll(anyList())).willReturn(List.of(buildSavedSpot(1L, "스팟")));

            spotAdminService.createSpots(request);

            ArgumentCaptor<List<Spot>> captor = ArgumentCaptor.forClass(List.class);
            then(spotRepository).should().saveAll(captor.capture());
            Spot spot = captor.getValue().get(0);
            assertThat(spot.getComment()).isEqualTo("설명입니다");
            assertThat(spot.getAddress()).isEqualTo("서울시 마포구");
            assertThat(spot.getGridNx()).isEqualTo(60);
            assertThat(spot.getGridNy()).isEqualTo(126);
            assertThat(spot.getCrowdAreaName()).isEqualTo("한강공원");
        }

        @Test
        @DisplayName("crowdAreaName 미입력 시 좌표로 최근접 장소가 자동 매핑된다")
        void shouldAutoMapCrowdAreaNameWhenBlank() {
            Item item = buildItem("광화문 스팟", SpotTheme.SUNSET, 37.5709, 126.9772);
            SpotAdminCreateRequest request = new SpotAdminCreateRequest(List.of(item));
            given(spotRepository.saveAll(anyList())).willReturn(List.of(buildSavedSpot(1L, "광화문 스팟")));
            given(crowdAreaMapper.findNearestAreaName(anyDouble(), anyDouble()))
                .willReturn(Optional.of("광화문·덕수궁"));

            spotAdminService.createSpots(request);

            ArgumentCaptor<List<Spot>> captor = ArgumentCaptor.forClass(List.class);
            then(spotRepository).should().saveAll(captor.capture());
            assertThat(captor.getValue().get(0).getCrowdAreaName()).isEqualTo("광화문·덕수궁");
        }

        @Test
        @DisplayName("crowdAreaName 직접 입력 시 자동 매핑하지 않고 입력값을 유지한다")
        void shouldRespectProvidedCrowdAreaName() {
            Item item = new Item("스팟", null, SpotTheme.SUNSET, 37.5, 127.0, null, null, null, "직접지정");
            SpotAdminCreateRequest request = new SpotAdminCreateRequest(List.of(item));
            given(spotRepository.saveAll(anyList())).willReturn(List.of(buildSavedSpot(1L, "스팟")));

            spotAdminService.createSpots(request);

            ArgumentCaptor<List<Spot>> captor = ArgumentCaptor.forClass(List.class);
            then(spotRepository).should().saveAll(captor.capture());
            assertThat(captor.getValue().get(0).getCrowdAreaName()).isEqualTo("직접지정");
            then(crowdAreaMapper).shouldHaveNoInteractions();
        }
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private Item buildItem(String name, SpotTheme theme, double lat, double lng) {
        return new Item(name, null, theme, lat, lng, null, null, null, null);
    }

    private Spot buildSavedSpot(Long id, String name) {
        Spot spot = Spot.builder()
            .name(name)
            .theme(SpotTheme.SUNSET)
            .latitude(37.55)
            .longitude(126.99)
            .status(SpotStatus.PUBLISHED)
            .build();
        ReflectionTestUtils.setField(spot, "id", id);
        return spot;
    }
}
