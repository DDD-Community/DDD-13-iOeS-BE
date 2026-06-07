package com.ioes.photo.global.config.s3.properties;

import com.ioes.photo.global.common.util.NullUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AWS S3 / MinIO 연결 설정 프로퍼티.
 *
 * bucket - 이미지를 저장할 S3 버킷 이름
 * region - AWS 리전
 * endpoint - 커스텀 엔드포인트. MinIO: http://localhost:9000, 실제 AWS: 빈 문자열
 * accessKey - Access Key MinIO: MINIO_ROOT_USER, AWS: IAM Access Key
 * secretKey- Secret Key MinIO: MINIO_ROOT_PASSWORD, AWS: IAM Secret Key
 * cloudFrontDomain - CloudFront 배포 도메인 (예: d1234567890.cloudfront.net)
 *
 * @author 황제연
 */
@ConfigurationProperties(prefix = "app.s3")
public record S3Properties(
    String bucket,
    String region,
    String endpoint,
    String accessKey,
    String secretKey,
    String cloudFrontDomain,
    String distributionId
) {
    public boolean hasCustomEndpoint() {
        return NullUtils.isNotBlank(endpoint);
    }


    public boolean hasCloudFront() {
        return NullUtils.isNotBlank(cloudFrontDomain);
    }
}
