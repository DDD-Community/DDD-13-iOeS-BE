package com.ioes.photo.external.kakao;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ioes.photo.external.kakao.dto.Coord2AddressResponse;
import com.ioes.photo.external.kakao.dto.KakaoAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class KakaoAddressTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    @DisplayName("coord2address JSON 응답을 파싱해 도로명/지번/시구 주소를 추출한다")
    void extractAddressFromResponse() throws Exception {
        String json = new ClassPathResource("external/kakao-coord2address-response.json")
            .getContentAsString(StandardCharsets.UTF_8);

        Coord2AddressResponse response = objectMapper.readValue(json, Coord2AddressResponse.class);
        Optional<KakaoAddress> address = KakaoAddress.from(response);

        assertThat(address).isPresent();
        assertThat(address.get().roadAddress()).isEqualTo("서울특별시 영등포구 여의공원로 68");
        assertThat(address.get().jibunAddress()).isEqualTo("서울특별시 영등포구 여의도동 18");
        assertThat(address.get().simpleAddress()).isEqualTo("서울특별시 영등포구");
    }

    @Test
    @DisplayName("documents가 비어 있으면 빈 결과를 반환한다")
    void emptyWhenNoDocuments() {
        Optional<KakaoAddress> address = KakaoAddress.from(new Coord2AddressResponse(List.of()));
        assertThat(address).isEmpty();
    }

    @Test
    @DisplayName("응답이 null이면 빈 결과를 반환한다")
    void emptyWhenNullResponse() {
        assertThat(KakaoAddress.from(null)).isEmpty();
    }

    @Test
    @DisplayName("도로명 주소가 없으면 지번 기준으로 시구 주소를 구성한다")
    void simpleAddressFallsBackToJibun() {
        Coord2AddressResponse response = new Coord2AddressResponse(List.of(
            new Coord2AddressResponse.Document(
                null,
                new Coord2AddressResponse.Address("부산광역시 해운대구 우동 1500", "부산광역시", "해운대구")
            )
        ));

        Optional<KakaoAddress> address = KakaoAddress.from(response);

        assertThat(address).isPresent();
        assertThat(address.get().roadAddress()).isNull();
        assertThat(address.get().jibunAddress()).isEqualTo("부산광역시 해운대구 우동 1500");
        assertThat(address.get().simpleAddress()).isEqualTo("부산광역시 해운대구");
    }
}
