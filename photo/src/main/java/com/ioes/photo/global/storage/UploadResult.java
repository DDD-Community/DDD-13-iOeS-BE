package com.ioes.photo.global.storage;

/**
 * 파일 업로드 결과.
 *
 * DB에는 key(S3 객체 키)만 저장합니다.
 * URL은 조회 시점에 으로 동적 생성합니다
 *
 * @param key              S3 객체 키 (경로 포함, URL 아님)
 * @param originalFilename UTF-8 정제된 원본 파일명
 * @param fileSize         파일 크기(bytes)
 * @param contentType      MIME 타입
 *
 * @author 황제연
 */
public record UploadResult(
    String key,
    String originalFilename,
    long fileSize,
    String contentType
) {}