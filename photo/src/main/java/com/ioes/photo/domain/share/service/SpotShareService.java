package com.ioes.photo.domain.share.service;

import com.ioes.photo.domain.share.dto.ShareView;
import com.ioes.photo.domain.share.util.SpotIdCoder;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.spot.service.SpotThumbnailService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공유 링크 토큰을 스팟 미리보기 정보로 해석하는 서비스.
 *
 * 토큰 난독화는 보안 수단이 아니므로 공개 가능한 스팟(PUBLISHED)만 노출한다.
 * 디코딩 실패·미존재·미공개 스팟은 모두 빈 결과로 처리되어 폴백 페이지로 이어진다.
 *
 * @author 김성민
 */
@Service
@RequiredArgsConstructor
public class SpotShareService {

    private final SpotRepository spotRepository;
    private final SpotImageRepository spotImageRepository;
    private final SpotThumbnailService spotThumbnailService;

    @Transactional(readOnly = true)
    public Optional<ShareView> findShareView(String token) {
        return SpotIdCoder.decodeSpotId(token)
            .flatMap(spotRepository::findById)
            .filter(spot -> spot.getStatus() == SpotStatus.PUBLISHED)
            .map(this::toShareView);
    }

    private ShareView toShareView(Spot spot) {
        String imageUrl = spotImageRepository.findById(spot.getId())
            .map(spotThumbnailService::getImageUrl)
            .orElse(null);
        return new ShareView(spot.getName(), spot.getComment(), imageUrl);
    }
}
