package com.ioes.photo.global.storage;

import com.ioes.photo.global.common.util.FilenameUtils;
import com.ioes.photo.global.common.util.FileUtils;
import com.ioes.photo.global.common.util.NullUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * S3 객체 키(경로) 생성 유틸리티.
 *
 * 경로 형식: {env}/{access}/{entity}/{entityId}/{type}/{yyyyMM}/{uuid}.{ext}
 *
 * 예시:
 * - prod/private/users/123/original/202504/abc123def456.jpg
 * - prod/public/spots/9901/thumbnail/202504/abc123def456.jpg
 *
 * @author 황제연
 */
public final class StoragePathUtils {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final int ACCESS_TYPE_INDEX = 1;

    private StoragePathUtils() {}

    public static String generate(String env, AccessType access, String entity,
                                  Long entityId, String type, String originalFilename) {
        String sanitized = FilenameUtils.sanitize(originalFilename);
        String ext = FileUtils.getExtension(sanitized);
        return buildKey(env, access, entity, entityId, type, ext);
    }

    public static String generateWithExt(String env, AccessType access, String entity,
                                          Long entityId, String type, String ext) {
        return buildKey(env, access, entity, entityId, type, ext);
    }

    public static boolean isPublic(String key) {
        if (NullUtils.isBlank(key)) {
            return false;
        }
        String[] parts = key.split("/", 3);
        return parts.length >= 2 && AccessType.PUBLIC.getValue().equals(parts[ACCESS_TYPE_INDEX]);
    }

    private static String buildKey(String env, AccessType access, String entity,
                                    Long entityId, String type, String ext) {
        String yyyyMM = LocalDate.now().format(MONTH_FORMATTER);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String filename = (NullUtils.isBlank(ext))
                ? uuid
                : uuid + "." + ext;
        return String.join("/", env, access.getValue(), entity,
                           String.valueOf(entityId), type, yyyyMM, filename);
    }
}