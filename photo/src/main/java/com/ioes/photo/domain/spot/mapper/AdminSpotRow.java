package com.ioes.photo.domain.spot.mapper;

import java.time.LocalDateTime;

/**
 * 어드민 검수 목록 MyBatis 조회 결과 Row.
 *
 * status 는 DB 코드값이며, 응답 변환 시 SpotStatus enum name 으로 변환한다.
 *
 * @author 황제연
 */
public record AdminSpotRow(
    Long id,
    LocalDateTime appliedAt,
    String userNickname,
    String name,
    String status,
    String handlerName,
    LocalDateTime handledAt
) {}
