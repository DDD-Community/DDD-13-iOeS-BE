package com.ioes.photo.domain.spot.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnumConverter;
import jakarta.persistence.Converter;

/**
 * @author 김성민
 */
@Converter(autoApply = true)
public class SpotSourceConverter extends CodedEnumConverter<SpotSource> {
    public SpotSourceConverter() {
        super(SpotSource.class);
    }
}
