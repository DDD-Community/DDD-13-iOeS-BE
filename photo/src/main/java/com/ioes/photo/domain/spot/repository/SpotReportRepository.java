package com.ioes.photo.domain.spot.repository;

import com.ioes.photo.domain.spot.entity.SpotReport;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 스팟 신고 JPA 리포지토리.
 *
 * @author 황제연
 */
public interface SpotReportRepository extends JpaRepository<SpotReport, Long> {
}
