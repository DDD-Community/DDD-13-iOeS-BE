package com.ioes.photo.domain.spot.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnumConverter;
import jakarta.persistence.Converter;

/**
 * 스팟 신고 상태 관련 ENUM converter
 *
 * @author 황제연
 */
@Converter(autoApply = true)
public class SpotReportStatusConverter extends CodedEnumConverter<SpotReportStatus> {
    public SpotReportStatusConverter() {
        super(SpotReportStatus.class);
    }
}
