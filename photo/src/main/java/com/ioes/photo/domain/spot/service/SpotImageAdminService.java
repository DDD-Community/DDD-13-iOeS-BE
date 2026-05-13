package com.ioes.photo.domain.spot.service;

import com.ioes.photo.domain.spot.dto.SpotImageSyncRequest;
import com.ioes.photo.domain.spot.dto.SpotImageSyncResponse;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.global.config.image.ImageProperties;
import com.ioes.photo.global.storage.AccessType;
import com.ioes.photo.global.storage.HeicImageResizer;
import com.ioes.photo.global.storage.ImageResizer;
import com.ioes.photo.global.storage.S3StorageService;
import com.ioes.photo.global.storage.StoragePathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 스팟 이미지 어드민 서비스.
 *
 * MVP 단계에서 S3에 직접 업로드된 이미지를 DB에 등록하고 썸네일을 생성한다.
 * 업로드 기능 추가 시 이 서비스의 썸네일 생성 로직이 업로드 파이프라인으로 이동한다.
 *
 * @author 황제연
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpotImageAdminService {

    private final SpotImageRepository spotImageRepository;
    private final S3StorageService s3StorageService;
    private final ImageResizer imageResizer;
    private final HeicImageResizer heicImageResizer;
    private final ImageProperties imageProperties;

    @Transactional
    public SpotImageSyncResponse syncImage(Long spotId, SpotImageSyncRequest request) {
        SpotImage spotImage = spotImageRepository.findById(spotId)
            .map(existing -> {
                existing.updateImageKey(request.imageKey());
                existing.updateOriginalFilename(request.originalFilename());
                existing.updateContentType(request.contentType());
                existing.updateRecordedDate(request.recordedDate());
                existing.updateRecordedTime(request.recordedTime());
                return existing;
            })
            .orElseGet(() -> {
                SpotImage created = SpotImage.create(
                    spotId, request.imageKey(), request.originalFilename(), request.contentType()
                );
                created.updateRecordedDate(request.recordedDate());
                created.updateRecordedTime(request.recordedTime());
                return created;
            });

        spotImage = spotImageRepository.save(spotImage);

        String thumbnailKey = generateAndUploadThumbnail(request, spotId);
        spotImage.updateThumbnailKey(thumbnailKey);

        log.info("이미지 동기화 완료: spotId={}, imageKey={}, thumbnailKey={}",
            spotId, request.imageKey(), thumbnailKey);

        return new SpotImageSyncResponse(
            s3StorageService.getUrl(request.imageKey()),
            s3StorageService.getUrl(thumbnailKey)
        );
    }

    private String generateAndUploadThumbnail(SpotImageSyncRequest request, Long spotId) {
        byte[] original = s3StorageService.fetchBytes(request.imageKey());
        int width = imageProperties.thumbnail().width();
        int height = imageProperties.thumbnail().height();

        byte[] thumbnail = heicImageResizer.supports(request.contentType())
            ? heicImageResizer.resize(original, width, height)
            : imageResizer.resize(original, width, height);

        String thumbnailKey = buildThumbnailKey(request.imageKey(), spotId);
        s3StorageService.uploadBytes(thumbnail, thumbnailKey, imageResizer.outputContentType());
        return thumbnailKey;
    }

    private String buildThumbnailKey(String originalKey, Long spotId) {
        String[] parts = originalKey.split("/");
        if (parts.length >= 7) {
            parts[4] = "thumbnail";
            String filename = parts[6];
            int dotIdx = filename.lastIndexOf('.');
            parts[6] = (dotIdx > 0 ? filename.substring(0, dotIdx) : filename) + ".jpg";
            return String.join("/", parts);
        }
        String env = parts.length > 0 ? parts[0] : "prod";
        AccessType access = StoragePathUtils.isPublic(originalKey) ? AccessType.PUBLIC : AccessType.PRIVATE;
        return StoragePathUtils.generateWithExt(env, access, "spots", spotId, "thumbnail", "jpg");
    }
}