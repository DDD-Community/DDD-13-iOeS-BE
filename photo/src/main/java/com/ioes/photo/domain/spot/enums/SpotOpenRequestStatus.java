package com.ioes.photo.domain.spot.enums;

import com.ioes.photo.global.common.util.NullUtils;
import com.ioes.photo.global.persistence.enumeration.CodedEnum;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 오픈 신청 이력의 처리 상태.
 *
 * REQUESTED 는 아직 결론이 나지 않은 진행 중 신청이며, 스팟당 최대 1건만 존재한다.
 * 나머지는 마감 상태로, 운영자 검수 결과(APPROVED/REJECTED)이거나 사용자의 철회(CANCELED)다.
 *
 * @author 황제연
 */
@Getter
@RequiredArgsConstructor
public enum SpotOpenRequestStatus implements CodedEnum {
    REQUESTED("Q"),
    APPROVED("A"),
    REJECTED("R"),
    CANCELED("C");

    private static final Map<String, SpotOpenRequestStatus> CODE_INDEX = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(SpotOpenRequestStatus::getCode, Function.identity()));

    private final String code;

    public static SpotOpenRequestStatus fromCode(String code) {
        SpotOpenRequestStatus status = NullUtils.isBlank(code) ? null : CODE_INDEX.get(code);
        if (status == null) {
            throw new IllegalArgumentException("알 수 없는 SpotOpenRequestStatus 코드: " + code);
        }
        return status;
    }
}
