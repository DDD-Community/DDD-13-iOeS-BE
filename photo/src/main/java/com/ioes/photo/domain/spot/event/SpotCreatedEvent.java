package com.ioes.photo.domain.spot.event;

/**
 * 스팟 생성 이벤트.
 *
 * 등록 트랜잭션 커밋 이후 날씨/천문 정보 즉시 수집을 트리거하기 위해 발행한다.
 *
 * @author 김성민
 */
public record SpotCreatedEvent(Long spotId) {
}
