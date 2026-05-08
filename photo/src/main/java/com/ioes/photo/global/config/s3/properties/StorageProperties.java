package com.ioes.photo.global.config.s3.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 스토리지 공통 설정 프로퍼티.
 *
 * type - 스토리지 타입 (현재: s3)
 * env  - S3 객체 키 경로에 사용할 환경 식별자 (dev / test / prod)
 *        Spring 활성 프로필과 독립적으로 명시하여, 다중 프로필 활성화 시에도 경로가 일정하게 유지됩니다.
 *
 * @author 황제연
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(String type, String env) {}