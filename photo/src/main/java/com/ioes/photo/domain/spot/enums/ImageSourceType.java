package com.ioes.photo.domain.spot.enums;

import com.ioes.photo.global.common.util.NullUtils;
import com.ioes.photo.global.persistence.enumeration.CodedEnum;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 스팟 이미지의 저장 출처 구분.
 *
 * INTERNAL은 자사 S3/MinIO 버킷에 업로드된 이미지로, {@code image_key}가 오브젝트 키다.
 * EXTERNAL은 관광공사류 오픈 API 등 외부에서 호스팅되는 이미지로, {@code image_key}에 완전한 URL을 그대로 저장하고
 * 조회 시에도 그 URL을 그대로 반환한다(hotlink). PUBLIC/PRIVATE 경로 이동·presigned URL 생성 대상이 아니다.
 * EXTERNAL 행은 애플리케이션 코드가 만들지 않으며, 데이터 적재용 SQL이 image_source_type='E'로 직접 세팅한다.
 * 애플리케이션은 이미 세팅된 값을 읽어 분기(SpotThumbnailService, SpotImageAccessService)하는 용도로만 사용한다.
 *
 * @author 황제연
 */
@Getter
@RequiredArgsConstructor
public enum ImageSourceType implements CodedEnum {
    INTERNAL("I"),
    EXTERNAL("E");

    private static final Map<String, ImageSourceType> CODE_INDEX = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(ImageSourceType::getCode, Function.identity()));

    private final String code;

    public static ImageSourceType fromCode(String code) {
        ImageSourceType type = NullUtils.isBlank(code) ? null : CODE_INDEX.get(code);
        if (type == null) {
            throw new IllegalArgumentException("알 수 없는 ImageSourceType 코드: " + code);
        }
        return type;
    }
}
