package com.ioes.photo.domain.alarm.service;

import com.ioes.photo.domain.alarm.dto.SpotAlarmResponse;
import com.ioes.photo.domain.alarm.entity.SpotAlarmSubscription;
import com.ioes.photo.domain.alarm.repository.SpotAlarmSubscriptionRepository;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.error.SpotErrorCode;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내 스팟 촬영조건 알림 구독 서비스.
 *
 * 스팟 등록 시 구독을 적재하고, 사용자가 본인 스팟의 알림 수신 여부를 토글할 수 있게 한다.
 * 실제 알림 발송 및 조건 평가는 추후 별도 기능으로 다룬다.
 *
 * @author 김성민
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpotAlarmService {

    private final SpotAlarmSubscriptionRepository subscriptionRepository;
    private final SpotRepository spotRepository;

    @Transactional
    public SpotAlarmResponse subscribe(Long userId, Long spotId) {
        SpotAlarmSubscription subscription = subscriptionRepository.findByUserIdAndSpotId(userId, spotId)
            .map(existing -> {
                existing.updateEnabled(true);
                return existing;
            })
            .orElseGet(() -> SpotAlarmSubscription.create(userId, spotId));
        return SpotAlarmResponse.of(subscriptionRepository.save(subscription));
    }

    @Transactional
    public SpotAlarmResponse updateEnabled(Long userId, Long spotId, boolean enabled) {
        verifyOwnership(userId, spotId);
        SpotAlarmSubscription subscription = subscriptionRepository.findByUserIdAndSpotId(userId, spotId)
            .map(existing -> {
                existing.updateEnabled(enabled);
                return existing;
            })
            .orElseGet(() -> SpotAlarmSubscription.builder()
                .userId(userId)
                .spotId(spotId)
                .enabled(enabled)
                .build());
        return SpotAlarmResponse.of(subscriptionRepository.save(subscription));
    }

    public SpotAlarmResponse getSubscription(Long userId, Long spotId) {
        verifyOwnership(userId, spotId);
        return subscriptionRepository.findByUserIdAndSpotId(userId, spotId)
            .map(SpotAlarmResponse::of)
            .orElseGet(() -> SpotAlarmResponse.disabled(spotId));
    }

    /**
     * 스팟이 삭제될 때 해당 스팟을 구독한 모든 알림을 끈다.
     * 구독 행 자체는 남겨 이후 알림 기능에서 사용자의 과거 설정을 참고할 수 있게 한다.
     */
    @Transactional
    public void disableBySpotId(Long spotId) {
        subscriptionRepository.findAllBySpotIdAndEnabledTrue(spotId)
            .forEach(subscription -> subscription.updateEnabled(false));
    }

    private void verifyOwnership(Long userId, Long spotId) {
        Spot spot = spotRepository.findById(spotId)
            .orElseThrow(() -> new BusinessException(SpotErrorCode.SPOT_NOT_FOUND));
        if (!userId.equals(spot.getUserId())) {
            throw new BusinessException(CommonErrorCode.ACCESS_DENIED,
                "본인이 등록한 스팟만 알림을 설정할 수 있습니다.");
        }
    }
}
