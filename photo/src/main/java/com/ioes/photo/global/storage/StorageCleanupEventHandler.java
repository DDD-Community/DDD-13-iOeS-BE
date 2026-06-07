package com.ioes.photo.global.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

/**
 * S3 파일 정리 이벤트 핸들러.
 *
 * StorageCleanupEvent: DB 커밋 완료 후 구 파일 삭제 + CloudFront 무효화 (AFTER_COMMIT)
 * StorageUploadRollbackEvent: DB 롤백 발생 시 신규 업로드 파일 보상 삭제 (AFTER_ROLLBACK)
 *
 * CloudFrontInvalidationService는 조건부 빈(app.s3.distribution-id 설정 시에만 생성)이므로 Optional로 주입받아,
 * 설정이 없는 환경에서도 정상 동작합니다.
 *
 * @author 황제연
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageCleanupEventHandler {

    private final StorageService storageService;
    private final Optional<CloudFrontInvalidationService> cloudFrontService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCleanup(StorageCleanupEvent event) {
        storageService.delete(event.key());
        cloudFrontService.ifPresent(service -> service.invalidate(event.key()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleUploadRollback(StorageUploadRollbackEvent event) {
        log.warn("트랜잭션 롤백으로 인한 S3 신규 파일 보상 삭제: key={}", event.key());
        storageService.delete(event.key());
    }
}