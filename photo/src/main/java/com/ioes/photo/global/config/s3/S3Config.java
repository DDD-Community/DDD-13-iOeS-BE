package com.ioes.photo.global.config.s3;

import com.ioes.photo.global.common.util.NullUtils;
import com.ioes.photo.global.config.s3.properties.S3Properties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * AWS S3 / MinIO 클라이언트 설정.
 *
 * app.storage.type=s3일 때만 활성화됩니다.
 * app.s3.endpoint가 설정된 경우 커스텀 엔드포인트(MinIO 등) 모드로 동작합니다.
 *
 * @author 황제연
 */
@Configuration
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3Config {

    @Bean
    public S3Client s3Client(S3Properties props) {
        var builder = S3Client.builder()
            .region(Region.of(props.region()))
            .credentialsProvider(credentialsProvider(props));

        if (props.hasCustomEndpoint()) {
            builder
                .endpointOverride(URI.create(props.endpoint()))
                .serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build());
        }

        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner(S3Properties props) {
        var builder = S3Presigner.builder()
            .region(Region.of(props.region()))
            .credentialsProvider(credentialsProvider(props));

        if (props.hasCustomEndpoint()) {
            builder
                .endpointOverride(URI.create(props.endpoint()))
                .serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build());
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.s3.distribution-id")
    public CloudFrontClient cloudFrontClient(S3Properties props) {
        return CloudFrontClient.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(credentialsProvider(props))
            .build();
    }

    private AwsCredentialsProvider credentialsProvider(S3Properties props) {
        if (NullUtils.isNotBlank(props.accessKey())) {
            return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.accessKey(), props.secretKey())
            );
        }
        return DefaultCredentialsProvider.builder().build();
    }
}