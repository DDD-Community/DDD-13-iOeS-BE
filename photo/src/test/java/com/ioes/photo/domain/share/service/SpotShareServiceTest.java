package com.ioes.photo.domain.share.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ioes.photo.domain.share.dto.ShareView;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spot.service.SpotThumbnailService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpotShareService 테스트")
class SpotShareServiceTest {

    private static final String SPOT_ID_1_TOKEN = "k-k";

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private SpotImageRepository spotImageRepository;

    @Mock
    private SpotThumbnailService spotThumbnailService;

    @InjectMocks
    private SpotShareService spotShareService;

    @Test
    @DisplayName("공개(PUBLISHED) 스팟은 이름·코멘트·이미지 URL을 반환한다")
    void returnsShareViewForPublishedSpot() {
        Spot spot = spotWith(SpotStatus.PUBLISHED, "남산 야경", "서울 야경 명소");
        SpotImage image = org.mockito.Mockito.mock(SpotImage.class);
        given(spotRepository.findById(1L)).willReturn(Optional.of(spot));
        given(spotImageRepository.findById(1L)).willReturn(Optional.of(image));
        given(spotThumbnailService.getImageUrl(image)).willReturn("https://cdn/spot.jpg");

        Optional<ShareView> result = spotShareService.findShareView(SPOT_ID_1_TOKEN);

        assertThat(result).contains(new ShareView("남산 야경", "서울 야경 명소", "https://cdn/spot.jpg"));
    }

    @ParameterizedTest(name = "{0} 스팟은 노출하지 않는다")
    @EnumSource(value = SpotStatus.class, names = {"PENDING", "REJECTED"})
    @DisplayName("미공개 스팟은 정보를 노출하지 않는다")
    void doesNotExposeNonPublishedSpot(SpotStatus status) {
        Spot spot = org.mockito.Mockito.mock(Spot.class);
        given(spot.getStatus()).willReturn(status);
        given(spotRepository.findById(1L)).willReturn(Optional.of(spot));

        Optional<ShareView> result = spotShareService.findShareView(SPOT_ID_1_TOKEN);

        assertThat(result).isEmpty();
        verifyNoInteractions(spotImageRepository, spotThumbnailService);
    }

    @Test
    @DisplayName("존재하지 않는 스팟은 빈 결과를 반환한다")
    void returnsEmptyForMissingSpot() {
        given(spotRepository.findById(1L)).willReturn(Optional.empty());

        assertThat(spotShareService.findShareView(SPOT_ID_1_TOKEN)).isEmpty();
    }

    @Test
    @DisplayName("디코딩 실패 토큰은 스팟을 조회하지 않는다")
    void skipsLookupForUndecodableToken() {
        assertThat(spotShareService.findShareView("not-a-valid-token")).isEmpty();

        verifyNoInteractions(spotRepository, spotImageRepository, spotThumbnailService);
    }

    private Spot spotWith(SpotStatus status, String name, String comment) {
        Spot spot = org.mockito.Mockito.mock(Spot.class);
        given(spot.getStatus()).willReturn(status);
        lenient().when(spot.getId()).thenReturn(1L);
        lenient().when(spot.getName()).thenReturn(name);
        lenient().when(spot.getComment()).thenReturn(comment);
        return spot;
    }
}
