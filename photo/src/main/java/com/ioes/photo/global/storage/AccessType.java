package com.ioes.photo.global.storage;

import lombok.Getter;

/**
 * S3 경로 내 접근 권한 구분
 *
 * PUBLIC - CloudFront 퍼블릭 배포 콘텐츠 (만료 없는 URL)
 * PRIVATE - Presigned URL 인증 필요 콘텐츠
 *
 *
 * @author 황제연
 */
@Getter
public enum AccessType {

    PUBLIC("public"),
    PRIVATE("private");

    private final String value;

    AccessType(String value) {
        this.value = value;
    }

}