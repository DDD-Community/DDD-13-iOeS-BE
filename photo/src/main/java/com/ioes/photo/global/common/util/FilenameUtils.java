package com.ioes.photo.global.common.util;

import java.nio.charset.StandardCharsets;

/**
 * HTTP multipart 파일명 정제 유틸리티.
 *
 * 한글 파일명 인코딩 문제
 * 일부 구형 HTTP 클라이언트는 Content-Disposition 헤더에서 파일명을 RFC 5987 (filename*=UTF-8''...) 방식이 아닌 단순 바이트 스트림으로 전송합니다.
 * 이 경우 Spring/Servlet 컨테이너가 ISO-8859-1로 파싱하면 한글이 깨집니다.
 *
 * 해결 전략
 * 1. S3 키에는 항상 UUID를 사용하여 파일명 인코딩 문제를 원천 차단합니다.
 * 2. 원본 파일명은 DB에 UTF-8로 저장하기 위해 ISO-8859-1 복원을 시도합니다.
 * 3. 경로 구분자(/,\)를 제거하여 Path Traversal 공격을 방지합니다.
 *
 * @author 황제연
 */
public final class FilenameUtils {

    private FilenameUtils() {}

    public static String sanitize(String originalFilename) {
        if (NullUtils.isBlank(originalFilename)) {
            return "unknown";
        }
        // Path Traversal 방지: 경로 구분자 제거
        String name = originalFilename.replaceAll("[/\\\\]", "_").trim();
        return tryRecoverEncoding(name);
    }


    private static String tryRecoverEncoding(String value) {
        try {
            byte[] bytes = value.getBytes(StandardCharsets.ISO_8859_1);
            boolean hasHighByte = false;
            for (byte b : bytes) {
                if ((b & 0xFF) > 0x7F) {
                    hasHighByte = true;
                    break;
                }
            }
            if (!hasHighByte) {
                return value; // ASCII 전용 — 변환 불필요
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value; // 복원 실패 시 원본 그대로
        }
    }
}