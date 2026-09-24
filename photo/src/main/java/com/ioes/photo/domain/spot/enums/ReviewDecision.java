package com.ioes.photo.domain.spot.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 스팟 검수 결정.
 *
 * 운영자가 오픈 신청 건을 검수한 결과이며, 리뷰 이력(SpotReview)에 함께 저장된다.
 *
 * @author 황제연
 */
@Getter
@RequiredArgsConstructor
public enum ReviewDecision implements CodedEnum {
    APPROVED("A"),
    REJECTED("R");

    private final String code;

    public boolean isApproved() {
        return this == APPROVED;
    }
}
