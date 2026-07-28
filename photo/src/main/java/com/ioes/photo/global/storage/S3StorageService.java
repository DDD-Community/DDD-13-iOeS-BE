package com.ioes.photo.global.storage;

import com.ioes.photo.global.common.util.FileUtils;
import com.ioes.photo.global.common.util.FilenameUtils;
import com.ioes.photo.global.common.util.ImageTypeDetector;
import com.ioes.photo.global.common.util.NullUtils;
import com.ioes.photo.global.config.file.properties.FileProperties;
import com.ioes.photo.global.config.image.ImageProperties;
import com.ioes.photo.global.config.s3.properties.S3Properties;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * AWS S3 / MinIO 기반 저장소 구현체.
 * URL 생성 전략
 *
 * MinIO(커스텀 엔드포인트): 직접 URL 반환 - 개발 환경 전용
 * Public + CloudFront 설정: 만료 없는 CloudFront URL 반환
 * Private 또는 CloudFront 미설정: Presigned URL 동적 생성
 *
 * 한글 파일명:
 * - S3 객체 키는 UUID로 생성하여 인코딩 문제를 원천 차단합니다.
 * - Content-Disposition 헤더에는 RFC 5987 (filename*=UTF-8''...) 인코딩을 적용합니다.
 *
 * @author 황제연
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;
    private final FileProperties fileProperties;
    private final ImageProperties imageProperties;

    @Override
    public UploadResult upload(MultipartFile file, String key) {
        FileUtils.validateNotEmpty(file);
        FileUtils.validateImage(file, fileProperties.imageExtensionSet());
        FileUtils.validateSize(file, fileProperties.maxSize());
        ImageTypeDetector.validate(file);

        putObject(file, key);
        return new UploadResult(
            key,
            FilenameUtils.sanitize(file.getOriginalFilename()),
            file.getSize(),
            resolveContentType(file)
        );
    }

    @Override
    public UploadResult uploadBytes(byte[] data, String key, String contentType) {
        try {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(s3Properties.bucket())
                    .key(key)
                    .contentType(contentType)
                    .contentLength((long) data.length)
                    .build(),
                RequestBody.fromBytes(data)
            );
        } catch (SdkException e) {
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR,
                "S3 업로드에 실패했습니다: " + e.getMessage());
        }
        return new UploadResult(key, null, data.length, contentType);
    }

    @Override
    public String getUrl(String key) {
        if (NullUtils.isBlank(key)) {
            return null;
        }

        // MinIO(개발 환경): 직접 URL — presigned 불필요
        if (s3Properties.hasCustomEndpoint()) {
            return s3Properties.endpoint() + "/" + s3Properties.bucket() + "/" + key;
        }

        // Public 콘텐츠 + CloudFront: 만료 없는 CDN URL
        if (StoragePathUtils.isPublic(key) && s3Properties.hasCloudFront()) {
            return "https://" + s3Properties.cloudFrontDomain() + "/" + key;
        }

        // Private 콘텐츠 또는 CloudFront 미설정: Presigned URL 동적 생성
        return generatePresignedUrl(key);
    }

    public byte[] fetchBytes(String key) {
        try {
            ResponseBytes<GetObjectResponse> response = s3Client.getObject(
                GetObjectRequest.builder()
                    .bucket(s3Properties.bucket())
                    .key(key)
                    .build(),
                ResponseTransformer.toBytes()
            );
            return response.asByteArray();
        } catch (SdkException e) {
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR,
                "S3 파일 읽기에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public void delete(String key) {
        if (NullUtils.isBlank(key)) {
            return;
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(key)
                .build());
        } catch (SdkException e) {
            // 삭제 실패는 치명적이지 않으므로 경고 로그만 남기고 계속 진행
            log.warn("S3 파일 삭제 실패 (무시됨): key={}, error={}", key, e.getMessage());
        }
    }

    private void putObject(MultipartFile file, String key) {
        // 파일 전체를 힙에 적재(file.getBytes())하지 않도록 디스크 임시파일로 옮긴 뒤 업로드한다.
        // RequestBody.fromFile 은 재시도 시 SDK 가 파일을 다시 열어 읽으므로(mark/reset 불필요),
        // 네트워크 불안정으로 인한 리트라이에서도 안전하고 contentLength 도 파일 크기로 정확히 잡힌다.
        Path temp = null;
        try {
            temp = Files.createTempFile("s3-upload-", ".tmp");
            file.transferTo(temp);
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(s3Properties.bucket())
                    .key(key)
                    .contentType(resolveContentType(file))
                    .contentLength(Files.size(temp))
                    .contentDisposition(buildContentDisposition(file.getOriginalFilename()))
                    .build(),
                    RequestBody.fromFile(temp)
            );
        } catch (IOException e) {
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR,
                "파일 처리에 실패했습니다.");
        } catch (SdkException e) {
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR,
                "S3 업로드에 실패했습니다: " + e.getMessage());
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException e) {
                    log.warn("S3 업로드 임시파일 삭제 실패: {}", temp.toAbsolutePath());
                }
            }
        }
    }

    private String generatePresignedUrl(String key) {
        var presignedRequest = s3Presigner.presignGetObject(
            GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(imageProperties.presignedUrlExpiryMinutes()))
                .getObjectRequest(GetObjectRequest.builder()
                    .bucket(s3Properties.bucket())
                    .key(key)
                    .build())
                .build()
        );
        return presignedRequest.url().toString();
    }

    private String resolveContentType(MultipartFile file) {
        String ct = file.getContentType();
        return (ct != null && !ct.isBlank()) ? ct : "application/octet-stream";
    }

    private String buildContentDisposition(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "attachment";
        }
        String sanitized = FilenameUtils.sanitize(originalFilename);
        String encoded = URLEncoder.encode(sanitized, StandardCharsets.UTF_8)
            .replace("+", "%20");
        return "attachment; filename*=UTF-8''" + encoded;
    }
}