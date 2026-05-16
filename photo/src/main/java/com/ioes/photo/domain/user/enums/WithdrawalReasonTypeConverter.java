package com.ioes.photo.domain.user.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnumConverter;
import jakarta.persistence.Converter;

/**
 * {@link WithdrawalReasonType} JPA 컨버터.
 *
 * @author 황제연
 */
@Converter(autoApply = true)
public class WithdrawalReasonTypeConverter extends CodedEnumConverter<WithdrawalReasonType> {

    public WithdrawalReasonTypeConverter() {
        super(WithdrawalReasonType.class);
    }
}
