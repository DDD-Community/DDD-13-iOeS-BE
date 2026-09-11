package com.ioes.photo.domain.spot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.global.storage.S3StorageService;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 외부 호스팅(EXTERNAL) 스팟 이미지 통합 테스트.
 *
 * EXTERNAL 행은 애플리케이션 코드가 만들지 않고 데이터 적재용 SQL이 image_source_type='E'로 직접 세팅한다.
 * 이 테스트는 그렇게 적재된 행을 실제 JPA(H2)로 읽었을 때 image_source_type 컨버터가 올바르게 매핑되고,
 * {@link SpotThumbnailService}가 자사 스토리지를 거치지 않고 저장된 URL을 그대로 서빙하는지 검증한다.
 *
 * @author 황제연
 */
@SpringBootTest
@DisplayName("스팟 이미지 출처 구분(EXTERNAL) 통합 테스트")
class SpotImageSourceTypeIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () ->
            "c2VjcmV0LWtleS1mb3ItdGVzdGluZy1wdXJwb3Nlcy1vbmx5LW11c3QtYmUtYXQtbGVhc3QtNjQtYnl0ZXMtbG9uZw==");
    }

    @MockitoBean RedisConnectionFactory redisConnectionFactory;
    @MockitoBean ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;
    @MockitoBean S3StorageService storageService;

    @Autowired SpotImageRepository spotImageRepository;
    @Autowired SpotThumbnailService spotThumbnailService;
    @Autowired JdbcTemplate jdbcTemplate;

    private static final Long SPOT_ID = 5001L;
    private static final String EXTERNAL_URL = "http://tong.visitkorea.or.kr/cms2/website/20/1961920.jpg";

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM spot_images");
    }

    @Test
    @DisplayName("SQL로 image_source_type='E' 행을 적재하면 엔티티가 EXTERNAL로 읽힌다")
    void loadsExternalRow_insertedDirectlyBySql() {
        jdbcTemplate.update(
            "INSERT INTO spot_images (spot_id, image_key, image_source_type, created_at, updated_at) "
                + "VALUES (?, ?, 'E', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            SPOT_ID, EXTERNAL_URL
        );

        Optional<SpotImage> found = spotImageRepository.findById(SPOT_ID);

        assertThat(found).isPresent();
        assertThat(found.get().isExternal()).isTrue();
        assertThat(found.get().getImageKey()).isEqualTo(EXTERNAL_URL);
    }

    @Test
    @DisplayName("SQL로 적재된 EXTERNAL 행은 자사 스토리지 호출 없이 저장된 URL을 그대로 서빙한다")
    void servesStoredUrlDirectly_withoutTouchingStorage() {
        jdbcTemplate.update(
            "INSERT INTO spot_images (spot_id, image_key, image_source_type, created_at, updated_at) "
                + "VALUES (?, ?, 'E', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            SPOT_ID, EXTERNAL_URL
        );
        SpotImage spotImage = spotImageRepository.findById(SPOT_ID).orElseThrow();

        String imageUrl = spotThumbnailService.getImageUrl(spotImage);
        String thumbnailUrl = spotThumbnailService.getThumbnailUrl(spotImage);

        assertThat(imageUrl).isEqualTo(EXTERNAL_URL);
        assertThat(thumbnailUrl).isEqualTo(EXTERNAL_URL);
        then(storageService).should(never()).getUrl(anyString());
    }

    @Test
    @DisplayName("image_source_type 컬럼을 지정하지 않고 적재된 행은 기본값 'I'(INTERNAL)로 읽힌다")
    void loadsInternalRow_whenColumnOmitted() {
        jdbcTemplate.update(
            "INSERT INTO spot_images (spot_id, image_key, image_source_type, created_at, updated_at) "
                + "VALUES (?, ?, 'I', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            SPOT_ID, "prod/public/spots/5001/original/202608/abc.jpg"
        );

        SpotImage found = spotImageRepository.findById(SPOT_ID).orElseThrow();

        assertThat(found.isExternal()).isFalse();
    }
}
