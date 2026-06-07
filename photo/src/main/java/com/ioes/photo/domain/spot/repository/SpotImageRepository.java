package com.ioes.photo.domain.spot.repository;

import com.ioes.photo.domain.spot.entity.SpotImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotImageRepository extends JpaRepository<SpotImage, Long> {

    List<SpotImage> findAllBySpotIdIn(List<Long> spotIds);
}
