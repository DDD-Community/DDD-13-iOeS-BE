package com.ioes.photo.global.common.util;

import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * 파일 처리 순수 정적 유틸리티 클래스
 * 설정값이 필요 없는 순수 파일 연산(확장자 추출, 파일명 생성, 검증, 삭제 등)을 제공합니다
 * 설정(업로드 경로, 최대 크기 등)에 의존하는 저장 연산은 FileService를 사용하세요
 *
 * @see FileService
 * @author 황제연
 */
public final class FileUtils {

    private FileUtils() {}

    /**
     * 확장자 추출, 없으면 빈 문자열 리턴
     */
    public static String getExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase();
    }

    /**
     * Path에서 확장자 추출
     * @param path
     * @return
     */
    public static String getExtension(Path path) {
        return getExtension(path.getFileName().toString());
    }

    /**
     * 확장자 제외한 파일명 반환
     * @param filename
     * @return
     */
    public static String getBaseName(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex < 0 ? filename : filename.substring(0, dotIndex);
    }

    /**
     * UUID 기반 파일명 생성. 원본 확장자 유지
     * @param originalFilename
     * @return
     */
    public static String generateFileName(String originalFilename) {
        String ext = getExtension(originalFilename);
        return ext.isEmpty()
            ? UUID.randomUUID().toString()
            : UUID.randomUUID() + "." + ext;
    }

    /**
     * 바이트를 가독성 있는 형식으로 변환 (예: "1.00 MB")
     * @param bytes
     * @return
     */
    public static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String unit = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.2f %s", bytes / Math.pow(1024, exp), unit);
    }


    /**
     * 이미지 파일 여부 확인
     * @param file
     * @param allowedExtensions
     * @return
     */
    public static boolean isImage(MultipartFile file, Set<String> allowedExtensions) {
        return allowedExtensions.contains(getExtension(file.getOriginalFilename()));
    }

    /**
     * 파일명 문자열로 이미지 여부 확인
     * @param filename
     * @param allowedExtensions
     * @return
     */
    public static boolean isImage(String filename, Set<String> allowedExtensions) {
        return allowedExtensions.contains(getExtension(filename));
    }

    /**
     * 파일이 비어있으면 예외 발생
     * @param file
     */
    public static void validateNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "파일이 비어있습니다.");
        }
    }

    /**
     * 파일 크기가 maxBytes 초과이면 예외 발생
     * @param file
     * @param maxBytes
     */
    public static void validateSize(MultipartFile file, long maxBytes) {
        if (file.getSize() > maxBytes) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE,
                "파일 크기가 허용 범위를 초과했습니다. (최대 " + formatSize(maxBytes) + ")");
        }
    }

    /**
     * 허용 이미지 확장자가 아니면 예외 발생
     * @param file
     * @param allowedExtensions
     */
    public static void validateImage(MultipartFile file, Set<String> allowedExtensions) {
        if (!isImage(file, allowedExtensions)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE,
                "이미지 파일만 업로드 가능합니다. (허용 확장자: " + allowedExtensions + ")");
        }
    }

    /**
     * 허용 확장자 외의 파일이면 {@code INVALID_INPUT_VALUE} 발생
     * @param file
     * @param allowedExtensions
     */
    public static void validateExtension(MultipartFile file, Set<String> allowedExtensions) {
        String ext = getExtension(file.getOriginalFilename());
        if (!allowedExtensions.contains(ext)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE,
                "허용되지 않는 파일 형식입니다. (허용 확장자: " + allowedExtensions + ")");
        }
    }

    /**
     * 지정 디렉터리에 파일을 저장합니다. 디렉터리가 없으면 자동 생성됩니다.
     *
     * @param file      저장할 파일
     * @param directory 저장 디렉터리
     * @param fileName  저장 파일명
     * @return 저장된 파일의 {@link Path}
     */
    public static Path saveToPath(MultipartFile file, Path directory, String fileName) {
        try {
            Files.createDirectories(directory);
            Path target = directory.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException e) {
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다.");
        }
    }

    /**
     * 파일 삭제. 존재하지 않으면 무시
     * @param filePath
     */
    public static void delete(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "파일 삭제에 실패했습니다.");
        }
    }

    /**
     * 디렉터리와 파일명으로 삭제
     * @param directory
     * @param fileName
     */
    public static void delete(Path directory, String fileName) {
        delete(directory.resolve(fileName));
    }

    /**
     * 파일이 존재하면 true
     * @param filePath
     * @return
     */
    public static boolean exists(Path filePath) {
        return Files.exists(filePath);
    }

    /**
     * 디렉터리와 파일명으로 존재 여부 확인
     * @param directory
     * @param fileName
     * @return
     */
    public static boolean exists(String directory, String fileName) {
        return exists(Paths.get(directory, fileName));
    }
}