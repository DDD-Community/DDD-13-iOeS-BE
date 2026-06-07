package com.ioes.photo.domain.spot.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnumConverter;
import jakarta.persistence.Converter;

/**
 * @author 김성민
 */
@Converter(autoApply = true)
public class SpotThemeConverter extends CodedEnumConverter<SpotTheme> {
    public SpotThemeConverter() {
        super(SpotTheme.class);
    }
}
