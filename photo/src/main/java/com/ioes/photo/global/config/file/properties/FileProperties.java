package com.ioes.photo.global.config.file.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 파일 업로드 관련 설정 프로퍼티 레코드.
 *
 * @param maxSize         허용 최대 파일 크기 (바이트 단위)
 * @param imageExtensions 허용 이미지 확장자 목록 (쉼표로 구분, 예: "jpg,jpeg,png")
 * @author 황제연
 */
@ConfigurationProperties(prefix = "app.file")
public record FileProperties(
    long maxSize,
    String imageExtensions
) {
    public Set<String> imageExtensionSet() {
        return Arrays.stream(imageExtensions.split(","))
            .map(String::trim)
            .collect(Collectors.toUnmodifiableSet());
    }
}
