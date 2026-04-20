package com.ioes.photo.domain.spotinfo.repository;

import com.ioes.photo.domain.spotinfo.entity.SpotInfo;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 스팟 최신 스냅샷(SpotInfo) JPA 리포지토리.
 *
 * @author 김성민
 */
public interface SpotInfoRepository extends JpaRepository<SpotInfo, Long> {
}
