package com.ioes.photo.domain.notification.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnumConverter;
import jakarta.persistence.Converter;

/**
 * @author 황제연
 */
@Converter(autoApply = true)
public class CheckYnConverter extends CodedEnumConverter<CheckYn> {
    public CheckYnConverter() {
        super(CheckYn.class);
    }
}
