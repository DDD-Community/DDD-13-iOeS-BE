package com.ioes.photo.domain.spot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.global.storage.StorageCleanupEvent;
import com.ioes.photo.global.storage.StorageService;
import com.ioes.photo.global.storage.StorageUploadRollbackEvent;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * {@link SpotImageAccessService} 단위 테스트.
 *
 * "공개 스팟의 이미지는 PUBLIC 경로에 있다"는 불변식이 승인/비공개 양방향에서 유지되는지 확인한다.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpotImageAccessService 단위 테스트")
class SpotImageAccessServiceTest {

    @Mock SpotImageRepository       spotImageRepository;
    @Mock StorageService            storageService;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks SpotImageAccessService spotImageAccessService;

    private static final Long SPOT_ID = 7L;
    private static final String PRIVATE_KEY   = "prod/private/spots/7/original/202607/abc.jpg";
    private static final String PRIVATE_THUMB = "prod/private/spots/7/thumbnail/202607/abc.jpg";
    private static final String PUBLIC_KEY    = "prod/public/spots/7/original/202607/abc.jpg";
    private static final String PUBLIC_THUMB  = "prod/public/spots/7/thumbnail/202607/abc.jpg";

    @Nested
    @DisplayName("공개 전환")
    class Publish {

        @Test
        @DisplayName("원본과 썸네일을 모두 public 경로로 옮긴다")
        void movesBothKeysToPublic() {
            SpotImage image = privateImageWithThumbnail();
            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.of(image));

            spotImageAccessService.publish(SPOT_ID);

            then(storageService).should().copy(PRIVATE_KEY, PUBLIC_KEY);
            then(storageService).should().copy(PRIVATE_THUMB, PUBLIC_THUMB);
            assertThat(image.getImageKey()).isEqualTo(PUBLIC_KEY);
            assertThat(image.getThumbnailKey()).isEqualTo(PUBLIC_THUMB);
        }

        @Test
        @DisplayName("원본 삭제는 즉시 수행하지 않고 커밋/롤백 이후로 미루는 이벤트만 발행한다")
        void defersStorageMutationToTransactionBoundary() {
            given(spotImageRepository.findById(SPOT_ID))
                .willReturn(Optional.of(SpotImage.create(SPOT_ID, PRIVATE_KEY)));

            spotImageAccessService.publish(SPOT_ID);

            then(storageService).should(never()).delete(anyString());
            then(eventPublisher).should().publishEvent(new StorageUploadRollbackEvent(PUBLIC_KEY));
            then(eventPublisher).should().publishEvent(new StorageCleanupEvent(PRIVATE_KEY));
        }

        @Test
        @DisplayName("이미 public 이면 아무것도 하지 않는다")
        void isIdempotent() {
            SpotImage image = SpotImage.create(SPOT_ID, PUBLIC_KEY);
            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.of(image));

            spotImageAccessService.publish(SPOT_ID);

            then(storageService).should(never()).copy(anyString(), anyString());
            assertThat(image.getImageKey()).isEqualTo(PUBLIC_KEY);
        }

        @Test
        @DisplayName("썸네일이 없으면 원본만 옮긴다")
        void skipsBlankThumbnail() {
            SpotImage image = SpotImage.create(SPOT_ID, PRIVATE_KEY);
            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.of(image));

            spotImageAccessService.publish(SPOT_ID);

            then(storageService).should().copy(PRIVATE_KEY, PUBLIC_KEY);
            assertThat(image.getThumbnailKey()).isNull();
        }

        @Test
        @DisplayName("이미지가 없는 스팟이면 아무것도 하지 않는다")
        void skipsWhenImageMissing() {
            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.empty());

            spotImageAccessService.publish(SPOT_ID);

            then(storageService).should(never()).copy(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("비공개 전환")
    class Unpublish {

        @Test
        @DisplayName("public 이미지를 private 경로로 되돌린다")
        void movesBothKeysBackToPrivate() {
            SpotImage image = SpotImage.create(SPOT_ID, PUBLIC_KEY);
            image.updateThumbnailKey(PUBLIC_THUMB);
            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.of(image));

            spotImageAccessService.unpublish(SPOT_ID);

            then(storageService).should().copy(PUBLIC_KEY, PRIVATE_KEY);
            then(storageService).should().copy(PUBLIC_THUMB, PRIVATE_THUMB);
            assertThat(image.getImageKey()).isEqualTo(PRIVATE_KEY);
            assertThat(image.getThumbnailKey()).isEqualTo(PRIVATE_THUMB);
        }

        @Test
        @DisplayName("되돌린 뒤 공개 경로의 사본을 정리하는 이벤트를 발행한다")
        void publishesCleanupForPublicKey() {
            given(spotImageRepository.findById(SPOT_ID))
                .willReturn(Optional.of(SpotImage.create(SPOT_ID, PUBLIC_KEY)));

            spotImageAccessService.unpublish(SPOT_ID);

            then(eventPublisher).should().publishEvent(new StorageUploadRollbackEvent(PRIVATE_KEY));
            then(eventPublisher).should().publishEvent(new StorageCleanupEvent(PUBLIC_KEY));
        }

        @Test
        @DisplayName("이미 private 이면 아무것도 하지 않는다")
        void isIdempotent() {
            SpotImage image = SpotImage.create(SPOT_ID, PRIVATE_KEY);
            given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.of(image));

            spotImageAccessService.unpublish(SPOT_ID);

            then(storageService).should(never()).copy(anyString(), anyString());
            assertThat(image.getImageKey()).isEqualTo(PRIVATE_KEY);
        }
    }

    @Test
    @DisplayName("접근 구분이 없는 비정상 경로는 이동하지 않고 그대로 둔다")
    void keepsMalformedKeyUnchanged() {
        SpotImage image = SpotImage.create(SPOT_ID, "legacy.jpg");
        given(spotImageRepository.findById(SPOT_ID)).willReturn(Optional.of(image));

        spotImageAccessService.publish(SPOT_ID);

        then(storageService).should(never()).copy(anyString(), anyString());
        assertThat(image.getImageKey()).isEqualTo("legacy.jpg");
    }

    private static SpotImage privateImageWithThumbnail() {
        SpotImage image = SpotImage.create(SPOT_ID, PRIVATE_KEY);
        image.updateThumbnailKey(PRIVATE_THUMB);
        return image;
    }
}
