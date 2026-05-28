package com.ioes.photo.domain.savedspot.service;

import com.ioes.photo.domain.savedspot.dto.BookmarkResponse;
import com.ioes.photo.domain.savedspot.dto.SavedSpotListResponse;
import com.ioes.photo.domain.savedspot.entity.SavedSpotArchive;
import com.ioes.photo.domain.savedspot.error.SavedSpotErrorCode;
import com.ioes.photo.domain.savedspot.mapper.SavedSpotMapper;
import com.ioes.photo.domain.savedspot.mapper.SavedSpotRow;
import com.ioes.photo.domain.savedspot.repository.SavedSpotArchiveRepository;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.error.SpotErrorCode;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.global.error.exception.BusinessException;
import com.ioes.photo.global.storage.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * {@link SavedSpotService} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SavedSpotService 단위 테스트")
class SavedSpotServiceTest {

    @Mock SavedSpotArchiveRepository savedSpotArchiveRepository;
    @Mock SpotRepository spotRepository;
    @Mock SpotImageRepository spotImageRepository;
    @Mock StorageService storageService;
    @Mock SavedSpotMapper savedSpotMapper;

    @InjectMocks SavedSpotService savedSpotService;

    private static final Long USER_ID = 1L;
    private static final Long SPOT_ID = 10L;

    // ── addBookmark ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addBookmark()")
    class AddBookmark {

        @Test
        @DisplayName("스팟이 존재하지 않으면 SPOT_NOT_FOUND 예외를 던진다")
        void throwsSpotNotFound_whenSpotMissing() {
            given(spotRepository.findByIdIncludingDeleted(SPOT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> savedSpotService.addBookmark(USER_ID, SPOT_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(SpotErrorCode.SPOT_NOT_FOUND));
        }

        @Test
        @DisplayName("스팟이 soft-delete되어 있으면 SPOT_DELETED 예외를 던진다")
        void throwsSpotDeleted_whenSpotSoftDeleted() {
            Spot deletedSpot = buildDeletedSpot();
            given(spotRepository.findByIdIncludingDeleted(SPOT_ID)).willReturn(Optional.of(deletedSpot));

            assertThatThrownBy(() -> savedSpotService.addBookmark(USER_ID, SPOT_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(SpotErrorCode.SPOT_DELETED));
        }

        @Test
        @DisplayName("이미 활성 북마크가 있으면 ALREADY_BOOKMARKED 예외를 던진다")
        void throwsAlreadyBookmarked_whenActiveArchiveExists() {
            Spot activeSpot = buildActiveSpot();
            given(spotRepository.findByIdIncludingDeleted(SPOT_ID)).willReturn(Optional.of(activeSpot));
            given(savedSpotArchiveRepository.findByUserIdAndSpotIdIncludingDeleted(USER_ID, SPOT_ID))
                .willReturn(Optional.of(buildActiveArchive()));

            assertThatThrownBy(() -> savedSpotService.addBookmark(USER_ID, SPOT_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(SavedSpotErrorCode.ALREADY_BOOKMARKED));
        }

        @Test
        @DisplayName("soft-delete된 북마크가 있으면 재활성화하고 bookmarkCount를 증가시킨다")
        void restoresDeletedArchive_andIncrementsCount() {
            SavedSpotArchive deleted = buildDeletedArchive();
            Spot activeSpot = buildActiveSpot();
            given(spotRepository.findByIdIncludingDeleted(SPOT_ID)).willReturn(Optional.of(activeSpot));
            given(savedSpotArchiveRepository.findByUserIdAndSpotIdIncludingDeleted(USER_ID, SPOT_ID))
                .willReturn(Optional.of(deleted));
            given(spotRepository.findBookmarkCountById(SPOT_ID)).willReturn(Optional.of(3L));

            BookmarkResponse response = savedSpotService.addBookmark(USER_ID, SPOT_ID);

            assertThat(deleted.isActive()).isTrue();
            then(spotRepository).should().incrementBookmarkCount(SPOT_ID);
            assertThat(response.bookmarkCount()).isEqualTo(3L);
        }

        @Test
        @DisplayName("기존 북마크가 없으면 신규 INSERT하고 bookmarkCount를 증가시킨다")
        void insertsNewArchive_andIncrementsCount() {
            Spot activeSpot = buildActiveSpot();
            given(spotRepository.findByIdIncludingDeleted(SPOT_ID)).willReturn(Optional.of(activeSpot));
            given(savedSpotArchiveRepository.findByUserIdAndSpotIdIncludingDeleted(USER_ID, SPOT_ID))
                .willReturn(Optional.empty());
            given(savedSpotArchiveRepository.saveAndFlush(any())).willReturn(buildActiveArchive());
            given(spotRepository.findBookmarkCountById(SPOT_ID)).willReturn(Optional.of(1L));

            BookmarkResponse response = savedSpotService.addBookmark(USER_ID, SPOT_ID);

            then(spotRepository).should().incrementBookmarkCount(SPOT_ID);
            assertThat(response.bookmarkCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("동시 INSERT로 DataIntegrityViolationException 발생 시 ALREADY_BOOKMARKED 예외로 변환된다")
        void convertsDataIntegrityViolation_toAlreadyBookmarked() {
            Spot activeSpot = buildActiveSpot();
            given(spotRepository.findByIdIncludingDeleted(SPOT_ID)).willReturn(Optional.of(activeSpot));
            given(savedSpotArchiveRepository.findByUserIdAndSpotIdIncludingDeleted(USER_ID, SPOT_ID))
                .willReturn(Optional.empty());
            given(savedSpotArchiveRepository.saveAndFlush(any()))
                .willThrow(new DataIntegrityViolationException("unique constraint"));

            assertThatThrownBy(() -> savedSpotService.addBookmark(USER_ID, SPOT_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(SavedSpotErrorCode.ALREADY_BOOKMARKED));

            then(spotRepository).should(never()).incrementBookmarkCount(any());
        }
    }

    // ── removeBookmark ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("removeBookmark()")
    class RemoveBookmark {

        @Test
        @DisplayName("스팟이 존재하지 않으면 SPOT_NOT_FOUND 예외를 던진다")
        void throwsSpotNotFound_whenSpotMissing() {
            given(spotRepository.findByIdIncludingDeleted(SPOT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> savedSpotService.removeBookmark(USER_ID, SPOT_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(SpotErrorCode.SPOT_NOT_FOUND));
        }

        @Test
        @DisplayName("활성 북마크가 없으면 NOT_BOOKMARKED 예외를 던진다")
        void throwsNotBookmarked_whenNoActiveArchive() {
            given(spotRepository.findByIdIncludingDeleted(SPOT_ID)).willReturn(Optional.of(mock(Spot.class)));
            given(savedSpotArchiveRepository.findByUserIdAndSpotId(USER_ID, SPOT_ID))
                .willReturn(Optional.empty());

            assertThatThrownBy(() -> savedSpotService.removeBookmark(USER_ID, SPOT_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(SavedSpotErrorCode.NOT_BOOKMARKED));
        }

        @Test
        @DisplayName("활성 북마크를 soft-delete하고 bookmarkCount를 감소시킨다")
        void softDeletesArchive_andDecrementsCount() {
            SavedSpotArchive active = buildActiveArchive();
            given(spotRepository.findByIdIncludingDeleted(SPOT_ID)).willReturn(Optional.of(mock(Spot.class)));
            given(savedSpotArchiveRepository.findByUserIdAndSpotId(USER_ID, SPOT_ID))
                .willReturn(Optional.of(active));
            given(spotRepository.findBookmarkCountById(SPOT_ID)).willReturn(Optional.of(0L));

            BookmarkResponse response = savedSpotService.removeBookmark(USER_ID, SPOT_ID);

            assertThat(active.isActive()).isFalse();
            then(spotRepository).should().decrementBookmarkCount(SPOT_ID);
            assertThat(response.bookmarkCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("soft-delete된 스팟도 북마크 해제가 가능하다")
        void allowsRemove_whenSpotSoftDeleted() {
            SavedSpotArchive active = buildActiveArchive();
            given(spotRepository.findByIdIncludingDeleted(SPOT_ID)).willReturn(Optional.of(mock(Spot.class)));
            given(savedSpotArchiveRepository.findByUserIdAndSpotId(USER_ID, SPOT_ID))
                .willReturn(Optional.of(active));
            given(spotRepository.findBookmarkCountById(SPOT_ID)).willReturn(Optional.of(0L));

            BookmarkResponse response = savedSpotService.removeBookmark(USER_ID, SPOT_ID);

            assertThat(active.isActive()).isFalse();
            assertThat(response.bookmarkCount()).isEqualTo(0L);
        }
    }

    // ── findSavedSpots ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("findSavedSpots()")
    class FindSavedSpots {

        @Test
        @DisplayName("위도만 전달해도 distanceKm=null로 정상 처리된다")
        void succeedsWithNullDistance_whenOnlyLatProvided() {
            given(savedSpotMapper.findSavedSpots(USER_ID, 37.5, null, 0, 6)).willReturn(List.of());
            given(savedSpotMapper.countSavedSpots(USER_ID)).willReturn(0L);

            SavedSpotListResponse response = savedSpotService.findSavedSpots(USER_ID, 0, 37.5, null);

            assertThat(response).isNotNull();
            assertThat(response.spots()).isEmpty();
        }

        @Test
        @DisplayName("경도만 전달해도 distanceKm=null로 정상 처리된다")
        void succeedsWithNullDistance_whenOnlyLngProvided() {
            given(savedSpotMapper.findSavedSpots(USER_ID, null, 127.0, 0, 6)).willReturn(List.of());
            given(savedSpotMapper.countSavedSpots(USER_ID)).willReturn(0L);

            SavedSpotListResponse response = savedSpotService.findSavedSpots(USER_ID, 0, null, 127.0);

            assertThat(response).isNotNull();
            assertThat(response.spots()).isEmpty();
        }

        @Test
        @DisplayName("위도/경도 모두 null이면 정상 처리된다")
        void successWhenBothNull() {
            given(savedSpotMapper.findSavedSpots(USER_ID, null, null, 0, 6)).willReturn(List.of());
            given(savedSpotMapper.countSavedSpots(USER_ID)).willReturn(0L);

            SavedSpotListResponse response = savedSpotService.findSavedSpots(USER_ID, 0, null, null);

            assertThat(response).isNotNull();
            assertThat(response.spots()).isEmpty();
        }

        @Test
        @DisplayName("조회된 스팟의 imageUrl은 StorageService.getUrl 결과다")
        void imageUrlIsFromStorageService() {
            SavedSpotRow row = buildRow(SPOT_ID, false);
            SpotImage image = SpotImage.create(SPOT_ID, "spots/1/image.jpg");
            given(savedSpotMapper.findSavedSpots(USER_ID, null, null, 0, 6)).willReturn(List.of(row));
            given(savedSpotMapper.countSavedSpots(USER_ID)).willReturn(1L);
            given(spotImageRepository.findAllBySpotIdIn(List.of(SPOT_ID))).willReturn(List.of(image));
            given(storageService.getUrl("spots/1/image.jpg")).willReturn("https://cdn.example.com/image.jpg");

            SavedSpotListResponse response = savedSpotService.findSavedSpots(USER_ID, 0, null, null);

            assertThat(response.spots()).hasSize(1);
            assertThat(response.spots().get(0).imageUrl()).isEqualTo("https://cdn.example.com/image.jpg");
        }

        @Test
        @DisplayName("이미지가 없는 스팟의 imageUrl은 null이다")
        void imageUrlIsNullWhenNoImage() {
            SavedSpotRow row = buildRow(SPOT_ID, false);
            given(savedSpotMapper.findSavedSpots(USER_ID, null, null, 0, 6)).willReturn(List.of(row));
            given(savedSpotMapper.countSavedSpots(USER_ID)).willReturn(1L);
            given(spotImageRepository.findAllBySpotIdIn(List.of(SPOT_ID))).willReturn(List.of());

            SavedSpotListResponse response = savedSpotService.findSavedSpots(USER_ID, 0, null, null);

            assertThat(response.spots().get(0).imageUrl()).isNull();
        }

        @Test
        @DisplayName("soft-delete된 스팟은 deleted=true로 표기된다")
        void deletedFlagIsTrue_whenSpotSoftDeleted() {
            SavedSpotRow row = buildRow(SPOT_ID, true);
            given(savedSpotMapper.findSavedSpots(USER_ID, null, null, 0, 6)).willReturn(List.of(row));
            given(savedSpotMapper.countSavedSpots(USER_ID)).willReturn(1L);
            given(spotImageRepository.findAllBySpotIdIn(List.of(SPOT_ID))).willReturn(List.of());

            SavedSpotListResponse response = savedSpotService.findSavedSpots(USER_ID, 0, null, null);

            assertThat(response.spots().get(0).deleted()).isTrue();
        }

        @Test
        @DisplayName("전체 개수가 다음 페이지 기준 초과 시 hasNext가 true다")
        void hasNextTrue_whenMorePages() {
            given(savedSpotMapper.findSavedSpots(USER_ID, null, null, 0, 6)).willReturn(List.of());
            given(savedSpotMapper.countSavedSpots(USER_ID)).willReturn(7L);

            SavedSpotListResponse response = savedSpotService.findSavedSpots(USER_ID, 0, null, null);

            assertThat(response.hasNext()).isTrue();
        }

        @Test
        @DisplayName("전체 개수가 다음 페이지 기준 이하면 hasNext가 false다")
        void hasNextFalse_whenLastPage() {
            given(savedSpotMapper.findSavedSpots(USER_ID, null, null, 0, 6)).willReturn(List.of());
            given(savedSpotMapper.countSavedSpots(USER_ID)).willReturn(6L);

            SavedSpotListResponse response = savedSpotService.findSavedSpots(USER_ID, 0, null, null);

            assertThat(response.hasNext()).isFalse();
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Spot buildActiveSpot() {
        Spot spot = mock(Spot.class);
        given(spot.getDeletedAt()).willReturn(null);
        return spot;
    }

    private Spot buildDeletedSpot() {
        Spot spot = mock(Spot.class);
        given(spot.getDeletedAt()).willReturn(LocalDateTime.now().minusDays(1));
        return spot;
    }

    private SavedSpotArchive buildActiveArchive() {
        SavedSpotArchive archive = SavedSpotArchive.builder().userId(USER_ID).spotId(SPOT_ID).build();
        ReflectionTestUtils.setField(archive, "id", 100L);
        return archive;
    }

    private SavedSpotArchive buildDeletedArchive() {
        SavedSpotArchive archive = buildActiveArchive();
        archive.softDelete();
        return archive;
    }

    private SavedSpotRow buildRow(Long spotId, boolean deleted) {
        return new SavedSpotRow(spotId, "테스트스팟", "SS", 37.5, 127.0, null, 0L, LocalDateTime.now(), deleted);
    }
}
