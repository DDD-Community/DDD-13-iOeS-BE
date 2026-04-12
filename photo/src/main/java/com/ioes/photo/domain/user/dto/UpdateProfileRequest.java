package com.ioes.photo.domain.user.dto;

/**
 * 프로필 업데이트 요청 DTO.
 *
 * null인 필드는 변경하지 않습니다.
 *
 * @param nickname 변경할 닉네임 (null이면 유지)
 * @param email    등록할 이메일 주소 (null이면 유지, 한 번 등록하면 변경 불가)
 * @author 황제연
 */
public record UpdateProfileRequest(
    String nickname,
    String email
) {}