package com.ioes.photo.domain.spot.enums;

/**
 * 스팟 공개 상태.
 *
 * 사용자 등록 스팟은 기본 PENDING 으로 생성되며, 운영자 검수 이후 PUBLISHED/REJECTED 로 전이된다.
 *
 * @author 김성민
 */
public enum SpotStatus {
    PENDING,
    PUBLISHED,
    REJECTED
}
