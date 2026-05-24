package com.ioes.photo.domain.alarm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ioes.photo.domain.alarm.dto.SpotAlarmResponse;
import com.ioes.photo.domain.alarm.entity.SpotAlarmSubscription;
import com.ioes.photo.domain.alarm.repository.SpotAlarmSubscriptionRepository;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.domain.spot.error.SpotErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link SpotAlarmService} 단위 테스트.
 *
 * @author 김성민
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpotAlarmService 단위 테스트")
class SpotAlarmServiceTest {

    @Mock SpotAlarmSubscriptionRepository subscriptionRepository;
    @Mock SpotRepository spotRepository;

    @InjectMocks SpotAlarmService spotAlarmService;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long SPOT_ID = 10L;

    private Spot spotOwnedBy(Long ownerId) {
        return Spot.builder()
            .name("한강 노을")
            .theme(SpotTheme.SUNSET)
            .latitude(37.5)
            .longitude(127.0)
            .userId(ownerId)
            .build();
    }

    @Nested
    @DisplayName("subscribe()")
    class Subscribe {

        @Test
        @DisplayName("구독 이력이 없으면 enabled=true로 새로 적재한다")
        void createsEnabledSubscription_whenNone() {
            given(subscriptionRepository.findByUserIdAndSpotId(USER_ID, SPOT_ID)).willReturn(Optional.empty());
            given(subscriptionRepository.save(any(SpotAlarmSubscription.class)))
                .willAnswer(inv -> inv.getArgument(0));

            SpotAlarmResponse response = spotAlarmService.subscribe(USER_ID, SPOT_ID);

            assertThat(response.spotId()).isEqualTo(SPOT_ID);
            assertThat(response.enabled()).isTrue();
        }

        @Test
        @DisplayName("기존 구독이 있으면 enabled=true로 재활성화한다")
        void reEnablesExistingSubscription() {
            SpotAlarmSubscription existing = SpotAlarmSubscription.builder()
                .userId(USER_ID).spotId(SPOT_ID).enabled(false).build();
            given(subscriptionRepository.findByUserIdAndSpotId(USER_ID, SPOT_ID)).willReturn(Optional.of(existing));
            given(subscriptionRepository.save(any(SpotAlarmSubscription.class)))
                .willAnswer(inv -> inv.getArgument(0));

            SpotAlarmResponse response = spotAlarmService.subscribe(USER_ID, SPOT_ID);

            assertThat(response.enabled()).isTrue();
            assertThat(existing.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("updateEnabled()")
    class UpdateEnabled {

        @Test
        @DisplayName("본인 스팟의 구독을 비활성화한다")
        void disablesOwnSpotSubscription() {
            SpotAlarmSubscription existing = SpotAlarmSubscription.builder()
                .userId(USER_ID).spotId(SPOT_ID).enabled(true).build();
            given(spotRepository.findById(SPOT_ID)).willReturn(Optional.of(spotOwnedBy(USER_ID)));
            given(subscriptionRepository.findByUserIdAndSpotId(USER_ID, SPOT_ID)).willReturn(Optional.of(existing));
            given(subscriptionRepository.save(any(SpotAlarmSubscription.class)))
                .willAnswer(inv -> inv.getArgument(0));

            SpotAlarmResponse response = spotAlarmService.updateEnabled(USER_ID, SPOT_ID, false);

            assertThat(response.enabled()).isFalse();
        }

        @Test
        @DisplayName("구독 이력이 없으면 요청한 enabled 값으로 새로 적재한다")
        void createsSubscription_whenNone() {
            given(spotRepository.findById(SPOT_ID)).willReturn(Optional.of(spotOwnedBy(USER_ID)));
            given(subscriptionRepository.findByUserIdAndSpotId(USER_ID, SPOT_ID)).willReturn(Optional.empty());
            given(subscriptionRepository.save(any(SpotAlarmSubscription.class)))
                .willAnswer(inv -> inv.getArgument(0));

            SpotAlarmResponse response = spotAlarmService.updateEnabled(USER_ID, SPOT_ID, true);

            ArgumentCaptor<SpotAlarmSubscription> captor = ArgumentCaptor.forClass(SpotAlarmSubscription.class);
            then(subscriptionRepository).should().save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
            assertThat(captor.getValue().getSpotId()).isEqualTo(SPOT_ID);
            assertThat(response.enabled()).isTrue();
        }

        @Test
        @DisplayName("타인 스팟이면 ACCESS_DENIED 예외를 던진다")
        void throwsAccessDenied_whenNotOwner() {
            given(spotRepository.findById(SPOT_ID)).willReturn(Optional.of(spotOwnedBy(OTHER_USER_ID)));

            assertThatThrownBy(() -> spotAlarmService.updateEnabled(USER_ID, SPOT_ID, true))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.ACCESS_DENIED);
        }

        @Test
        @DisplayName("스팟이 없으면 SPOT_NOT_FOUND 예외를 던진다")
        void throwsSpotNotFound_whenSpotMissing() {
            given(spotRepository.findById(SPOT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> spotAlarmService.updateEnabled(USER_ID, SPOT_ID, true))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SpotErrorCode.SPOT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getSubscription()")
    class GetSubscription {

        @Test
        @DisplayName("구독이 있으면 해당 상태를 반환한다")
        void returnsExistingState() {
            SpotAlarmSubscription existing = SpotAlarmSubscription.builder()
                .userId(USER_ID).spotId(SPOT_ID).enabled(true).build();
            given(spotRepository.findById(SPOT_ID)).willReturn(Optional.of(spotOwnedBy(USER_ID)));
            given(subscriptionRepository.findByUserIdAndSpotId(USER_ID, SPOT_ID)).willReturn(Optional.of(existing));

            SpotAlarmResponse response = spotAlarmService.getSubscription(USER_ID, SPOT_ID);

            assertThat(response.enabled()).isTrue();
        }

        @Test
        @DisplayName("구독 이력이 없으면 enabled=false로 반환한다")
        void returnsDisabled_whenNone() {
            given(spotRepository.findById(SPOT_ID)).willReturn(Optional.of(spotOwnedBy(USER_ID)));
            given(subscriptionRepository.findByUserIdAndSpotId(USER_ID, SPOT_ID)).willReturn(Optional.empty());

            SpotAlarmResponse response = spotAlarmService.getSubscription(USER_ID, SPOT_ID);

            assertThat(response.spotId()).isEqualTo(SPOT_ID);
            assertThat(response.enabled()).isFalse();
        }

        @Test
        @DisplayName("타인 스팟이면 ACCESS_DENIED 예외를 던진다")
        void throwsAccessDenied_whenNotOwner() {
            given(spotRepository.findById(SPOT_ID)).willReturn(Optional.of(spotOwnedBy(OTHER_USER_ID)));

            assertThatThrownBy(() -> spotAlarmService.getSubscription(USER_ID, SPOT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.ACCESS_DENIED);
        }
    }
}
