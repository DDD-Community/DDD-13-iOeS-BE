package com.ioes.photo.domain.notification.repository;

import com.ioes.photo.domain.notification.entity.SpotOpenReviewHistory;
import com.ioes.photo.domain.notification.enums.CheckYn;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 스팟 검수완료 알림 히스토리 리포지토리.
 *
 * @author 황제연
 */
public interface SpotOpenReviewHistoryRepository extends JpaRepository<SpotOpenReviewHistory, Long> {

    List<SpotOpenReviewHistory> findByUserIdAndCheckYnOrderByCreatedAtDesc(Long userId, CheckYn checkYn);
}
