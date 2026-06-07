package com.ioes.photo.global.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 저장소 인터페이스.
 *
 * URL 관리 전략
 * - DB에는 S3 객체 키만 저장하고, URL은 조회 시점에 동적으로 생성합니다.
 * 이를 통해 Presigned URL 만료 문제를 해결합니다
 * - Public 콘텐츠 + CloudFront 설정 시 -> 만료 없는 CloudFront URL 반환
 * - Private 콘텐츠 또는 CloudFront 미설정 시 -> Presigned URL 동적 생성
 * - 개발(MinIO) 환경 -> MinIO 직접 URL 반환
 *
 * @author 황제연
 */
public interface StorageService {

    UploadResult upload(MultipartFile file, String key);

    UploadResult uploadBytes(byte[] data, String key, String contentType);

    String getUrl(String key);

    void delete(String key);
}