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
 * StorageCleanupEvent를 수신하여 DB 트랜잭션 커밋 완료 후 S3 파일 삭제와CloudFront 캐시 무효화를 수행합니다.
 * AFTER_COMMIT 페이즈를 사용하므로 트랜잭션이 롤백되면 이벤트 자체가 무시되어 S3 파일이 불필요하게 삭제되지 않습니다.
 * CloudFrontInvalidationService는 조건부 빈(@code app.s3.distribution-id 설정 시에만 생성)이므로 Optional로 주입받아, 설정이 없는 환경에서도 정상 동작합니다.
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
}