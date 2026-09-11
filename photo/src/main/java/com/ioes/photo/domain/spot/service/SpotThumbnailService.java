package com.ioes.photo.domain.spot.service;

import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.global.common.util.NullUtils;
import com.ioes.photo.global.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 스팟 썸네일 URL 제공 서비스.
 *
 * MVP에서는 S3 직접 삽입 시 썸네일을 미리 생성해두는 방식을 사용한다.
 * 업로드 기능 추가 시 이 서비스에 업로드 시점 리사이징 로직이 추가된다.
 *
 * @author 황제연
 */
@Service
@RequiredArgsConstructor
public class SpotThumbnailService {

    private final StorageService storageService;

    public String getThumbnailUrl(SpotImage spotImage) {
        // 외부 호스팅 이미지는 별도 썸네일을 생성하지 않으므로 원본 URL을 그대로 내려준다.
        if (spotImage.isExternal()) {
            return getImageUrl(spotImage);
        }
        if (NullUtils.isBlank(spotImage.getThumbnailKey())) {
            return null;
        }
        return storageService.getUrl(spotImage.getThumbnailKey());
    }

    public String getImageUrl(SpotImage spotImage) {
        if (NullUtils.isBlank(spotImage.getImageKey())) {
            return null;
        }
        // 외부 호스팅 이미지는 image_key에 완전한 URL이 저장되어 있으므로 그대로 반환한다(hotlink).
        if (spotImage.isExternal()) {
            return spotImage.getImageKey();
        }
        return storageService.getUrl(spotImage.getImageKey());
    }
}