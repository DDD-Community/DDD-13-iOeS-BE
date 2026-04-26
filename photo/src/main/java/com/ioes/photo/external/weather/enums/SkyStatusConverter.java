package com.ioes.photo.external.weather.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnumConverter;
import jakarta.persistence.Converter;

/**
 * @author 김성민
 */
@Converter(autoApply = true)
public class SkyStatusConverter extends CodedEnumConverter<SkyStatus> {
    public SkyStatusConverter() {
        super(SkyStatus.class);
    }
}
