package com.ioes.photo.global.storage;

/**
 * S3 파일 정리 이벤트.
 *
 * 트랜잭션 커밋 이후 StorageCleanupEventHandler에 의해 처리됩니다.
 * S3 삭제와 CloudFront 무효화를 DB 커밋 성공 이후로 미루어,
 * 트랜잭션 롤백 시 스토리지 파일이 불필요하게 삭제되는 것을 방지합니다.
 *
 * @param key 삭제할 S3 객체 키
 * @author 황제연
 */
public record StorageCleanupEvent(String key) {}