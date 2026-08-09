package com.ioes.photo.domain.statistics.mapper;

/**
 * 로그인 경로(provider)별 가입자 수 집계 Row.
 *
 * @param provider    OAuth provider 코드 ('K' 카카오, 'A' 애플)
 * @param signupCount 해당 provider 가입자 수
 * @author 김성민
 */
public record ProviderCountRow(String provider, long signupCount) {
}
