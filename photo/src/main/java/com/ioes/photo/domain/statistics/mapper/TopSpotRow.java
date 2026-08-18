package com.ioes.photo.domain.statistics.mapper;

/**
 * 저장 수 상위 스팟 집계 Row.
 *
 * @param spotName  스팟 이름
 * @param saveCount 활성 저장(북마크) 수
 * @author 김성민
 */
public record TopSpotRow(String spotName, long saveCount) {
}
