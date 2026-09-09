package com.ioes.photo.domain.crowdarea.repository;

import com.ioes.photo.domain.crowdarea.entity.CrowdArea;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 혼잡도 장소 참조 데이터 리포지토리.
 *
 * @author 김성민
 */
public interface CrowdAreaRepository extends JpaRepository<CrowdArea, String> {

    List<CrowdArea> findAllByCategory(String category);
}
