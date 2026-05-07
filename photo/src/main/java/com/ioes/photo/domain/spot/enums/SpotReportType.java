package com.ioes.photo.domain.spot.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 스팟 신고 유형.
 *
 * @author 황제연
 */
@Getter
@RequiredArgsConstructor
public enum SpotReportType implements CodedEnum {
    LOCATION_ERROR("LO", "위치 오류"),
    WRONG_NAME("WN", "잘못된 이름"),
    ETC("ET", "기타");

    private final String code;
    private final String description;
}
