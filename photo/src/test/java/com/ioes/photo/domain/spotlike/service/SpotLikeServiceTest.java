package com.ioes.photo.domain.spotlike.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.error.SpotErrorCode;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spotlike.dto.SpotLikeResponse;
import com.ioes.photo.domain.spotlike.entity.SpotLike;
import com.ioes.photo.domain.spotlike.error.SpotLikeErrorCode;
import com.ioes.photo.domain.spotlike.repository.SpotLikeRepository;
import com.ioes.photo.global.error.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * {@link SpotLikeService} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpotLikeService 단위 테스트")
class SpotLikeServiceTest {

    @Mock SpotLikeRepository spotLikeRepository;
    @Mock SpotRepository     spotRepository;

    @InjectMocks SpotLikeService spotLikeService;

    private static final Long USER_ID = 7L;
    private static final Long SPOT_ID = 1L;

    @Nested
    @DisplayName("좋아요 등록")
    class AddLike {

        @Test
        @DisplayName("처음 누르면 좋아요를 저장하고 카운터를 1 올린다")
        void savesLikeAndIncrementsCount() {
            givenSpot(publishedUserSpot());
            given(spotLikeRepository.restoreLike(USER_ID, SPOT_ID)).willReturn(0);
            given(spotRepository.findLikeCountById(SPOT_ID)).willReturn(Optional.of(4L));

            SpotLikeResponse response = spotLikeService.addLike(USER_ID, SPOT_ID);

            then(spotLikeRepository).should().saveAndFlush(any(SpotLike.class));
            then(spotRepository).should().incrementLikeCount(SPOT_ID);
            assertThat(response.likeCount()).isEqualTo(4L);
            assertThat(response.isLiked()).isTrue();
        }

        @Test
        @DisplayName("취소했던 좋아요는 새로 저장하지 않고 되살린다")
        void restoresSoftDeletedLike() {
            givenSpot(publishedUserSpot());
            given(spotLikeRepository.restoreLike(USER_ID, SPOT_ID)).willReturn(1);
            given(spotRepository.findLikeCountById(SPOT_ID)).willReturn(Optional.of(1L));

            spotLikeService.addLike(USER_ID, SPOT_ID);

            then(spotLikeRepository).should(never()).saveAndFlush(any());
            then(spotRepository).should().incrementLikeCount(SPOT_ID);
        }

        @Test
        @DisplayName("이미 좋아요한 스팟이면 ALREADY_LIKED 예외를 던지고 카운터를 올리지 않는다")
        void throwsWhenAlreadyLiked() {
            givenSpot(publishedUserSpot());
            given(spotLikeRepository.restoreLike(USER_ID, SPOT_ID)).willReturn(0);
            given(spotLikeRepository.saveAndFlush(any(SpotLike.class)))
                .willThrow(new DataIntegrityViolationException("uk_spot_likes_user_spot"));

            assertThatThrownBy(() -> spotLikeService.addLike(USER_ID, SPOT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SpotLikeErrorCode.ALREADY_LIKED);

            then(spotRepository).should(never()).incrementLikeCount(anyLong());
        }

        @Test
        @DisplayName("동시에 두 요청이 들어와 UNIQUE 제약을 위반하면 ALREADY_LIKED 예외로 변환한다")
        void translatesUniqueViolationToAlreadyLiked() {
            givenSpot(publishedUserSpot());
            given(spotLikeRepository.restoreLike(USER_ID, SPOT_ID)).willReturn(0);
            given(spotLikeRepository.saveAndFlush(any(SpotLike.class)))
                .willThrow(new DataIntegrityViolationException("uk_spot_likes_user_spot"));

            assertThatThrownBy(() -> spotLikeService.addLike(USER_ID, SPOT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SpotLikeErrorCode.ALREADY_LIKED);
        }
    }

    @Nested
    @DisplayName("좋아요 허용 조건")
    class Likeable {

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = SpotStatus.class, names = "PUBLISHED", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("승인되지 않은 유저 스팟에는 좋아요할 수 없다")
        void rejectsUnpublishedUserSpot(SpotStatus status) {
            givenSpot(userSpot(status));

            assertThatThrownBy(() -> spotLikeService.addLike(USER_ID, SPOT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SpotLikeErrorCode.SPOT_NOT_LIKEABLE);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(SpotStatus.class)
        @DisplayName("관리자 큐레이션 스팟은 상태와 무관하게 좋아요할 수 있다")
        void allowsCuratedSpotRegardlessOfStatus(SpotStatus status) {
            givenSpot(curatedSpot(status));
            given(spotLikeRepository.restoreLike(USER_ID, SPOT_ID)).willReturn(0);
            given(spotRepository.findLikeCountById(SPOT_ID)).willReturn(Optional.of(1L));

            SpotLikeResponse response = spotLikeService.addLike(USER_ID, SPOT_ID);

            assertThat(response.isLiked()).isTrue();
        }

        @Test
        @DisplayName("삭제된 스팟에는 좋아요할 수 없다")
        void rejectsDeletedSpot() {
            Spot deleted = publishedUserSpot();
            ReflectionTestUtils.setField(deleted, "deletedAt", LocalDateTime.now());
            givenSpot(deleted);

            assertThatThrownBy(() -> spotLikeService.addLike(USER_ID, SPOT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SpotErrorCode.SPOT_DELETED);
        }

        @Test
        @DisplayName("존재하지 않는 스팟이면 SPOT_NOT_FOUND 예외를 던진다")
        void throwsWhenSpotNotFound() {
            given(spotRepository.findByIdIncludingDeleted(SPOT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> spotLikeService.addLike(USER_ID, SPOT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SpotErrorCode.SPOT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("좋아요 취소")
    class RemoveLike {

        @Test
        @DisplayName("좋아요를 논리삭제하고 카운터를 1 내린다")
        void softDeletesLikeAndDecrementsCount() {
            givenSpot(publishedUserSpot());
            given(spotLikeRepository.softDeleteLike(USER_ID, SPOT_ID)).willReturn(1);
            given(spotRepository.findLikeCountById(SPOT_ID)).willReturn(Optional.of(2L));

            SpotLikeResponse response = spotLikeService.removeLike(USER_ID, SPOT_ID);

            then(spotRepository).should().decrementLikeCount(SPOT_ID);
            assertThat(response.likeCount()).isEqualTo(2L);
            assertThat(response.isLiked()).isFalse();
        }

        @Test
        @DisplayName("좋아요하지 않은 스팟이면 NOT_LIKED 예외를 던진다")
        void throwsWhenNotLiked() {
            givenSpot(publishedUserSpot());
            given(spotLikeRepository.softDeleteLike(USER_ID, SPOT_ID)).willReturn(0);

            assertThatThrownBy(() -> spotLikeService.removeLike(USER_ID, SPOT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SpotLikeErrorCode.NOT_LIKED);

            then(spotRepository).should(never()).decrementLikeCount(anyLong());
        }

        @Test
        @DisplayName("비공개로 전환된 스팟이라도 이미 누른 좋아요는 취소할 수 있다")
        void allowsCancelOnUnpublishedSpot() {
            givenSpot(userSpot(SpotStatus.DRAFT));
            given(spotLikeRepository.softDeleteLike(USER_ID, SPOT_ID)).willReturn(1);
            given(spotRepository.findLikeCountById(SPOT_ID)).willReturn(Optional.of(0L));

            spotLikeService.removeLike(USER_ID, SPOT_ID);

            then(spotRepository).should().decrementLikeCount(SPOT_ID);
        }
    }

    private void givenSpot(Spot spot) {
        given(spotRepository.findByIdIncludingDeleted(SPOT_ID)).willReturn(Optional.of(spot));
    }

    private static Spot publishedUserSpot() {
        return userSpot(SpotStatus.PUBLISHED);
    }

    private static Spot userSpot(SpotStatus status) {
        return buildSpot(status, 42L);
    }

    private static Spot curatedSpot(SpotStatus status) {
        return buildSpot(status, null);
    }

    private static Spot buildSpot(SpotStatus status, Long ownerId) {
        Spot spot = Spot.builder()
            .name("테스트스팟")
            .theme(SpotTheme.SUNSET)
            .latitude(37.5)
            .longitude(127.0)
            .status(status)
            .userId(ownerId)
            .build();
        ReflectionTestUtils.setField(spot, "id", SPOT_ID);
        return spot;
    }
}
