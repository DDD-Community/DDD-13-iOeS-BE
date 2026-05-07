package com.ioes.photo.domain.spot.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 스팟 신고 처리 상태.
 *
 * @author 황제연
 */
@Getter
@RequiredArgsConstructor
public enum SpotReportStatus implements CodedEnum {
    PENDING("P", "대기중"),
    RESOLVED("R", "처리완료");

    private final String code;
    private final String description;
}
