package com.ioes.photo.domain.spot.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnumConverter;
import jakarta.persistence.Converter;

/**
 * {@link ImageSourceType} 코드 변환기.
 *
 * @author 황제연
 */
@Converter(autoApply = true)
public class ImageSourceTypeConverter extends CodedEnumConverter<ImageSourceType> {

    public ImageSourceTypeConverter() {
        super(ImageSourceType.class);
    }
}
