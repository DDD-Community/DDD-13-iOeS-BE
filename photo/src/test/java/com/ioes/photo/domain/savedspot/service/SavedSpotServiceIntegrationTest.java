package com.ioes.photo.domain.savedspot.service;

import com.ioes.photo.domain.savedspot.dto.BookmarkResponse;
import com.ioes.photo.domain.savedspot.entity.SavedSpotArchive;
import com.ioes.photo.domain.savedspot.error.SavedSpotErrorCode;
import com.ioes.photo.domain.savedspot.repository.SavedSpotArchiveRepository;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.error.SpotErrorCode;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.global.error.exception.BusinessException;
import com.ioes.photo.global.storage.S3StorageService;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SavedSpotService} 통합 테스트 — 실제 JPA + H2로 북마크 로직 검증.
 *
 * @author 황제연
 */
@SpringBootTest
@DisplayName("SavedSpotService 통합 테스트")
class SavedSpotServiceIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () ->
            "c2VjcmV0LWtleS1mb3ItdGVzdGluZy1wdXJwb3Nlcy1vbmx5LW11c3QtYmUtYXQtbGVhc3QtNjQtYnl0ZXMtbG9uZw==");
    }

    @MockitoBean RedisConnectionFactory redisConnectionFactory;
    @MockitoBean ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;
    @MockitoBean S3StorageService storageService;

    @Autowired SavedSpotService savedSpotService;
    @Autowired SavedSpotArchiveRepository savedSpotArchiveRepository;
    @Autowired SpotRepository spotRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private static final Long USER_ID = 1L;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM saved_spot_archives");
        jdbcTemplate.execute("DELETE FROM spots");
    }

    @Test
    @DisplayName("북마크 지정 시 saved_spot_archives에 레코드가 저장되고 bookmark_count가 1 증가한다")
    void addBookmark_savesArchiveAndIncrementsCount() {
        Spot spot = spotRepository.save(buildSpot());

        BookmarkResponse response = savedSpotService.addBookmark(USER_ID, spot.getId());

        SavedSpotArchive archive = savedSpotArchiveRepository
            .findByUserIdAndSpotId(USER_ID, spot.getId()).orElseThrow();
        assertThat(archive.isActive()).isTrue();
        assertThat(response.bookmarkCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("북마크 해제 시 deleted_at이 설정되고 bookmark_count가 0으로 감소한다")
    void removeBookmark_softDeletesArchiveAndDecrementsCount() {
        Spot spot = spotRepository.save(buildSpot());
        savedSpotService.addBookmark(USER_ID, spot.getId());

        BookmarkResponse response = savedSpotService.removeBookmark(USER_ID, spot.getId());

        SavedSpotArchive archive = savedSpotArchiveRepository
            .findByUserIdAndSpotIdIncludingDeleted(USER_ID, spot.getId()).orElseThrow();
        assertThat(archive.isActive()).isFalse();
        assertThat(response.bookmarkCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("북마크 해제 후 재북마크하면 재활성화되고 bookmark_count가 다시 1이 된다")
    void reAddBookmark_restoresArchive() {
        Spot spot = spotRepository.save(buildSpot());
        savedSpotService.addBookmark(USER_ID, spot.getId());
        savedSpotService.removeBookmark(USER_ID, spot.getId());

        BookmarkResponse response = savedSpotService.addBookmark(USER_ID, spot.getId());

        SavedSpotArchive archive = savedSpotArchiveRepository
            .findByUserIdAndSpotId(USER_ID, spot.getId()).orElseThrow();
        assertThat(archive.isActive()).isTrue();
        assertThat(response.bookmarkCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("이미 북마크된 스팟을 다시 북마크하면 ALREADY_BOOKMARKED 예외를 던진다")
    void addBookmark_throws_whenAlreadyBookmarked() {
        Spot spot = spotRepository.save(buildSpot());
        savedSpotService.addBookmark(USER_ID, spot.getId());

        assertThatThrownBy(() -> savedSpotService.addBookmark(USER_ID, spot.getId()))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(SavedSpotErrorCode.ALREADY_BOOKMARKED));
    }

    @Test
    @DisplayName("북마크되지 않은 스팟 해제 시 NOT_BOOKMARKED 예외를 던진다")
    void removeBookmark_throws_whenNotBookmarked() {
        Spot spot = spotRepository.save(buildSpot());

        assertThatThrownBy(() -> savedSpotService.removeBookmark(USER_ID, spot.getId()))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(SavedSpotErrorCode.NOT_BOOKMARKED));
    }

    @Test
    @DisplayName("존재하지 않는 스팟을 북마크하면 SPOT_NOT_FOUND 예외를 던진다")
    void addBookmark_throws_whenSpotNotFound() {
        assertThatThrownBy(() -> savedSpotService.addBookmark(USER_ID, 9999L))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(SpotErrorCode.SPOT_NOT_FOUND));
    }

    @Test
    @DisplayName("soft-delete된 스팟을 북마크하면 SPOT_DELETED 예외를 던진다")
    void addBookmark_throws_whenSpotSoftDeleted() {
        Spot spot = spotRepository.save(buildSpot());
        softDeleteSpot(spot.getId());

        assertThatThrownBy(() -> savedSpotService.addBookmark(USER_ID, spot.getId()))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(SpotErrorCode.SPOT_DELETED));
    }

    @Test
    @DisplayName("soft-delete된 스팟의 북마크는 해제할 수 있다")
    void removeBookmark_succeeds_whenSpotSoftDeleted() {
        Spot spot = spotRepository.save(buildSpot());
        savedSpotService.addBookmark(USER_ID, spot.getId());
        softDeleteSpot(spot.getId());

        BookmarkResponse response = savedSpotService.removeBookmark(USER_ID, spot.getId());

        SavedSpotArchive archive = savedSpotArchiveRepository
            .findByUserIdAndSpotIdIncludingDeleted(USER_ID, spot.getId()).orElseThrow();
        assertThat(archive.isActive()).isFalse();
        assertThat(response.bookmarkCount()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("bookmark_count는 0 미만으로 내려가지 않는다")
    void bookmarkCount_neverGoesNegative() {
        Spot spot = spotRepository.save(buildSpot());
        savedSpotService.addBookmark(USER_ID, spot.getId());
        savedSpotService.removeBookmark(USER_ID, spot.getId());

        long count = spotRepository.findBookmarkCountById(spot.getId()).orElseThrow();
        assertThat(count).isGreaterThanOrEqualTo(0L);
    }

    private Spot buildSpot() {
        return Spot.builder()
            .name("통합테스트스팟")
            .theme(SpotTheme.SUNSET)
            .latitude(37.5)
            .longitude(127.0)
            .status(SpotStatus.PUBLISHED)
            .build();
    }

    private void softDeleteSpot(Long spotId) {
        jdbcTemplate.update("UPDATE spots SET deleted_at = ? WHERE id = ?",
            LocalDateTime.now(), spotId);
    }
}
