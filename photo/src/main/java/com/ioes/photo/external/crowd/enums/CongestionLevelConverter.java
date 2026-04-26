package com.ioes.photo.external.crowd.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnumConverter;
import jakarta.persistence.Converter;

/**
 * @author 김성민
 */
@Converter(autoApply = true)
public class CongestionLevelConverter extends CodedEnumConverter<CongestionLevel> {
    public CongestionLevelConverter() {
        super(CongestionLevel.class);
    }
}
