package com.ioes.photo.global.common.util;

import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * 이미지 파일의 매직 바이트(파일 시그니처)를 검사하는 유틸리티.
 *
 * 필요성:
 * - 확장자는 파일명 변경으로 쉽게 위조할 수 있습니다.
 * - 악의적 파일(예: PHP 스크립트 -> .jpg 위장이 서버에 업로드되는 것을 방지하려면 파일 내용(매직 바이트)을 함께 검증해야 합니다
 * 지원 포맷 및 매직 바이트
 * JPEG: FF D8 FF
 * PNG: 89 50 4E 47 0D 0A 1A 0A
 * GIF: 47 49 46 38 37/39
 * BMP: 42 4D
 * WebP: 52 49 46 46 xx xx xx xx 57 45 42 50}
 * HEIC/HEIF: offset 4에 66 74 79 70 (ISO BMFF ftyp 박스)
 * SVG: 텍스트 기반 - <svg> 또는 <?xml> 접두사
 * EXIF는 추후 추가 예정
 *
 * 파일 타입 허용 범위 정책 수립 후, 디텍팅 범위 수정할 예정
 *
 * @author 황제연
 */
public final class ImageTypeDetector {

    private static final int HEADER_SIZE = 16;

    private static final byte[] JPEG_MAGIC = {(byte)0xFF, (byte)0xD8, (byte)0xFF};
    private static final byte[] PNG_MAGIC = {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] GIF87_MAGIC = {0x47, 0x49, 0x46, 0x38, 0x37};
    private static final byte[] GIF89_MAGIC = {0x47, 0x49, 0x46, 0x38, 0x39};
    private static final byte[] BMP_MAGIC = {0x42, 0x4D};
    private static final byte[] RIFF_MAGIC = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_MARKER = {0x57, 0x45, 0x42, 0x50};
    private static final byte[] FTYP_BOX = {0x66, 0x74, 0x79, 0x70};

    private static final Set<String> SVG_EXTENSIONS = Set.of("svg");

    private ImageTypeDetector() {}

    public static void validate(MultipartFile file) {
        String ext = FileUtils.getExtension(file.getOriginalFilename());

        if (SVG_EXTENSIONS.contains(ext)) {
            validateSvgContent(file);
            return;
        }

        byte[] header = readHeader(file);
        DetectedType detected = detectType(header);

        if (detected == DetectedType.UNKNOWN) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE,
                "파일 내용이 이미지 형식이 아닙니다. 실제 이미지 파일을 업로드하세요.");
        }

        if (!detected.matchesExtension(ext)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE,
                String.format("파일 확장자(.%s)와 실제 이미지 형식(%s)이 일치하지 않습니다.",
                    ext, detected.name().toLowerCase()));
        }
    }


    private static DetectedType detectType(byte[] header) {
        if (header.length < 4) {
            return DetectedType.UNKNOWN;
        }

        if (startsWith(header, JPEG_MAGIC)) {
            return DetectedType.JPEG;
        }
        if (startsWith(header, PNG_MAGIC)) {
            return DetectedType.PNG;
        }
        if (startsWith(header, GIF87_MAGIC) || startsWith(header, GIF89_MAGIC)) {
            return DetectedType.GIF;
        }
        if (startsWith(header, BMP_MAGIC)) {
            return DetectedType.BMP;
        }
        if (startsWith(header, RIFF_MAGIC) && header.length >= 12
            && matchesAt(header, WEBP_MARKER, 8)) {
            return DetectedType.WEBP;
        }
        if (header.length >= 8 && matchesAt(header, FTYP_BOX, 4)) {
            return DetectedType.HEIF;
        }

        return DetectedType.UNKNOWN;
    }

    private static void validateSvgContent(MultipartFile file) {
        byte[] header = readHeader(file);
        if (!isSvgStart(header)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE,
                "파일 내용이 SVG 형식이 아닙니다.");
        }
    }

    private static boolean isSvgStart(byte[] header) {
        if (header.length < 4) {
            return false;
        }
        // UTF-8 BOM (EF BB BF) 건너뛰기
        int offset = (header[0] == (byte)0xEF && header[1] == (byte)0xBB && header[2] == (byte)0xBF) ? 3 : 0;
        if (header.length <= offset + 4) {
            return false;
        }
        if (header[offset] == 0x3C && header[offset+1] == 0x73
            && header[offset+2] == 0x76 && header[offset+3] == 0x67) {
            return true;
        }
        return header[offset] == 0x3C && header[offset+1] == 0x3F
            && header[offset+2] == 0x78 && header[offset+3] == 0x6D;
    }

    private static byte[] readHeader(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(HEADER_SIZE);
        } catch (IOException e) {
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR,
                "파일 읽기 중 오류가 발생했습니다.");
        }
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesAt(byte[] data, byte[] pattern, int offset) {
        if (data.length < offset + pattern.length) {
            return false;
        }
        for (int i = 0; i < pattern.length; i++) {
            if (data[offset + i] != pattern[i]) {
                return false;
            }
        }
        return true;
    }

    private enum DetectedType {
        JPEG(Set.of("jpg", "jpeg", "exif")),
        PNG(Set.of("png")),
        GIF(Set.of("gif")),
        BMP(Set.of("bmp")),
        WEBP(Set.of("webp")),
        HEIF(Set.of("heic", "heif")),
        UNKNOWN(Set.of());

        private final Set<String> validExtensions;

        DetectedType(Set<String> extensions) {
            this.validExtensions = extensions;
        }

        boolean matchesExtension(String ext) {
            return ext != null && validExtensions.contains(ext.toLowerCase());
        }
    }
}