package com.ioes.photo.global.storage;

import com.ioes.photo.global.config.s3.properties.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationRequest;
import software.amazon.awssdk.services.cloudfront.model.InvalidationBatch;
import software.amazon.awssdk.services.cloudfront.model.Paths;

import java.util.Arrays;
import java.util.UUID;

/**
 * CloudFront 캐시 무효화 서비스.
 *
 * 적용 대상:
 * - CloudFront를 통해 제공되는 PUBLIC 콘텐츠에만 무효화가 의미 있습니다.
 * - PRIVATE 콘텐츠(Presigned URL 방식)는 CloudFront 캐시를 사용하지 않으므로 무효화가 불필요합니다.
 *
 * 비동기 처리:
 * 무효화 API 호출이 비동기(@Async)로 처리되므로 메인 요청 흐름을 차단하지 않습니다
 *
 * 활성화 조건
 * app.s3.distribution-id 가 설정된 경우에만 빈이 생성됩니다.
 *
 * @author 황제연
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(CloudFrontClient.class)
public class CloudFrontInvalidationService {

    private final CloudFrontClient cloudFrontClient;
    private final S3Properties s3Properties;

    @Async
    public void invalidate(String... keys) {
        String[] publicKeys = Arrays.stream(keys)
            .filter(StoragePathUtils::isPublic)
            .toArray(String[]::new);

        if (publicKeys.length == 0) {
            return;
        }

        String[] paths = Arrays.stream(publicKeys)
            .map(k -> "/" + k)
            .toArray(String[]::new);

        try {
            cloudFrontClient.createInvalidation(
                CreateInvalidationRequest.builder()
                    .distributionId(s3Properties.distributionId())
                    .invalidationBatch(
                        InvalidationBatch.builder()
                            .paths(Paths.builder()
                                .quantity(paths.length)
                                .items(paths)
                                .build())
                            .callerReference(UUID.randomUUID().toString())
                            .build()
                    )
                    .build()
            );
        } catch (SdkException e) {
            // 로그 남기는 것 이외, 예외전파는 무시한다.
            log.warn("CloudFront 캐시 무효화 실패 (무시됨): paths={}, error={}",
                Arrays.toString(paths), e.getMessage());
        }
    }
}