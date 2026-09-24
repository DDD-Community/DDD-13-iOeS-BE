package com.ioes.photo.domain.spot.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 스팟 반려 사유.
 *
 * {@code label} 은 운영자(어드민)에게 보여줄 짧은 사유명이고,
 * {@code guideMessage} 는 사용자(앱)에게 보여줄 안내 문구다.
 * ETC(기타)는 안내 문구가 없으며 운영자가 입력한 detail 을 그대로 노출하고, detail 입력이 필수다.
 *
 * @author 황제연
 */
@Getter
@RequiredArgsConstructor
public enum RejectionReason implements CodedEnum {
    DUPLICATE("D1", "중복 스팟", "이미 등록되어 있는 스팟과 매우 유사해요."),
    LOW_QUALITY("D2", "사진 상태 불량", "사진 화질이 낮거나 흔들려서 스팟을 확인하기 어려워요."),
    LOCATION_MISMATCH("D3", "위치 무관", "첨부하신 사진이 신청하신 위치와 일치하지 않는 것 같아요."),
    FILTER_MISMATCH("D4", "필터 불일치", "선택하신 필터와 사진이 맞지 않는 것 같아요."),
    ETC("D9", "기타", null);

    private final String code;
    private final String label;
    private final String guideMessage;

    public boolean requiresDetail() {
        return this == ETC;
    }
}
