package com.ioes.photo.domain.spot.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnumConverter;
import jakarta.persistence.Converter;

/**
 * {@link SpotOpenRequestStatus} 코드 변환기.
 *
 * @author 황제연
 */
@Converter(autoApply = true)
public class SpotOpenRequestStatusConverter extends CodedEnumConverter<SpotOpenRequestStatus> {

    public SpotOpenRequestStatusConverter() {
        super(SpotOpenRequestStatus.class);
    }
}
