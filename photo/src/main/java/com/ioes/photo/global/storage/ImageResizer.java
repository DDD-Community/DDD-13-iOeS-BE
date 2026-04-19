package com.ioes.photo.global.storage;

import com.ioes.photo.domain.storage.error.StorageErrorCode;
import com.ioes.photo.global.common.util.NullUtils;
import com.ioes.photo.global.error.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.tasks.UnsupportedFormatException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

/**
 * 이미지 썸네일 생성 컴포넌트
 *
 * @author 황제연
 */
@Slf4j
@Component
public class ImageResizer {

    private static final Set<String> SUPPORTED_TYPES = Set.of(
        "image/jpeg", "image/jpg", "image/png", "image/gif",
        "image/bmp", "image/webp"
    );

    private static final String OUTPUT_FORMAT = "jpeg";
    private static final String OUTPUT_CONTENT_TYPE = "image/jpeg";
    private static final double OUTPUT_QUALITY = 0.85;

    public byte[] resize(MultipartFile file, int width, int height) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Thumbnails.of(file.getInputStream())
                .size(width, height)
                .keepAspectRatio(true)
                .outputFormat(OUTPUT_FORMAT)
                .outputQuality(OUTPUT_QUALITY)
                .toOutputStream(out);
            return out.toByteArray();
        } catch (UnsupportedFormatException e) {
            throw new BusinessException(StorageErrorCode.UNSUPPORTED_IMAGE_FORMAT,
                "썸네일 변환을 지원하지 않는 이미지 포맷입니다: " + e.getMessage());
        } catch (IOException e) {
            throw new BusinessException(StorageErrorCode.THUMBNAIL_GENERATION_FAILED,
                "썸네일 생성 중 오류가 발생했습니다.");
        }
    }

    public boolean supports(String contentType) {
        if (NullUtils.isBlank(contentType)) {
            return false;
        }
        return SUPPORTED_TYPES.contains(contentType.toLowerCase());
    }

    public String outputContentType() {
        return OUTPUT_CONTENT_TYPE;
    }
}