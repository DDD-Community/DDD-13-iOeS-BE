package com.ioes.photo.domain.alarm.repository;

import com.ioes.photo.domain.alarm.entity.SpotAlarmSubscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 내 스팟 알림 구독 JPA 리포지토리.
 *
 * @author 김성민
 */
public interface SpotAlarmSubscriptionRepository extends JpaRepository<SpotAlarmSubscription, Long> {

    Optional<SpotAlarmSubscription> findByUserIdAndSpotId(Long userId, Long spotId);

    List<SpotAlarmSubscription> findAllByUserIdAndEnabledTrue(Long userId);

    List<SpotAlarmSubscription> findAllBySpotIdAndEnabledTrue(Long spotId);
}
