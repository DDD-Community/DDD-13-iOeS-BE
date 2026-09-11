package com.ioes.photo.domain.spot.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnumConverter;
import jakarta.persistence.Converter;

/**
 * @author 황제연
 */
@Converter(autoApply = true)
public class ReviewDecisionConverter extends CodedEnumConverter<ReviewDecision> {
    public ReviewDecisionConverter() {
        super(ReviewDecision.class);
    }
}
