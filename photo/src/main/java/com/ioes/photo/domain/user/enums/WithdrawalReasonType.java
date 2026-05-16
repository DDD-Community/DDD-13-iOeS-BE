package com.ioes.photo.domain.user.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 회원탈퇴 사유 유형.
 *
 * @author 황제연
 */
@Getter
@RequiredArgsConstructor
public enum WithdrawalReasonType implements CodedEnum {

    OTHERS("01");

    private final String code;
}
