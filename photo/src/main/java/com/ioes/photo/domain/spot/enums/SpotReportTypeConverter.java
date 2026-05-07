package com.ioes.photo.domain.spot.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnumConverter;
import jakarta.persistence.Converter;

/**
 * 스팟 신고 유형 관련 Converter
 *
 * @author 황제연
 */
@Converter(autoApply = true)
public class SpotReportTypeConverter extends CodedEnumConverter<SpotReportType> {
    public SpotReportTypeConverter() {
        super(SpotReportType.class);
    }
}
