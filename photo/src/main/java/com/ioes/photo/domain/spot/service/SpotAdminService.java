package com.ioes.photo.domain.spot.service;

import com.ioes.photo.domain.crowdarea.service.CrowdAreaMapper;
import com.ioes.photo.domain.spot.dto.SpotAdminCreateRequest;
import com.ioes.photo.domain.spot.dto.SpotAdminCreateRequest.Item;
import com.ioes.photo.domain.spot.dto.SpotAdminCreateResponse;
import com.ioes.photo.domain.spot.dto.SpotAdminCreateResponse.SpotResult;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.event.SpotCreatedEvent;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.external.weather.util.LccGridConverter;
import com.ioes.photo.external.weather.util.LccGridConverter.GridPoint;
import com.ioes.photo.global.common.util.NullUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 스팟 어드민 서비스.
 *
 * MVP 단계 운영 목적의 내부 전용 서비스로, 어드민이 스팟을 직접 등록할 수 있다.
 * 등록된 스팟은 즉시 PUBLISHED 상태로 공개된다.
 *
 * @author 황제연
 */
@Service
@RequiredArgsConstructor
public class SpotAdminService {

    private final SpotRepository spotRepository;
    private final CrowdAreaMapper crowdAreaMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public SpotAdminCreateResponse createSpots(SpotAdminCreateRequest request) {
        List<Spot> spots = request.spots().stream()
            .map(this::toSpot)
            .toList();

        List<Spot> saved = spotRepository.saveAll(spots);
        saved.forEach(spot -> eventPublisher.publishEvent(new SpotCreatedEvent(spot.getId())));

        List<SpotResult> results = saved.stream()
            .map(spot -> new SpotResult(spot.getId(), spot.getName()))
            .toList();

        return SpotAdminCreateResponse.of(results);
    }

    private Spot toSpot(Item item) {
        GridPoint grid = resolveGrid(item);
        return Spot.builder()
            .name(item.name())
            .comment(item.comment())
            .theme(item.theme())
            .latitude(item.latitude())
            .longitude(item.longitude())
            .address(item.address())
            .status(SpotStatus.PUBLISHED)
            .gridNx(grid.nx())
            .gridNy(grid.ny())
            .crowdAreaName(resolveCrowdAreaName(item))
            .build();
    }

    private GridPoint resolveGrid(Item item) {
        if (item.gridNx() != null && item.gridNy() != null) {
            return new GridPoint(item.gridNx(), item.gridNy());
        }
        return LccGridConverter.toGrid(item.latitude(), item.longitude());
    }

    private String resolveCrowdAreaName(Item item) {
        if (NullUtils.isNotBlank(item.crowdAreaName())) {
            return item.crowdAreaName();
        }
        return crowdAreaMapper.findNearestAreaName(item.latitude(), item.longitude()).orElse(null);
    }
}
