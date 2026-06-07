package com.ioes.photo.global.storage;

/**
 * S3 업로드 롤백 보상 이벤트.
 *
 * 트랜잭션 롤백 시 StorageCleanupEventHandler에 의해 처리됩니다.
 * S3 PUT 이후 DB 커밋이 실패(DB 락, 동시성, 커넥션 끊김 등)하면
 * 이미 업로드된 신규 파일을 삭제하여 orphan 파일 발생을 방지합니다.
 *
 * @param key 삭제할 S3 객체 키 (업로드 직후 발행)
 * @author 황제연
 */
public record StorageUploadRollbackEvent(String key) {}