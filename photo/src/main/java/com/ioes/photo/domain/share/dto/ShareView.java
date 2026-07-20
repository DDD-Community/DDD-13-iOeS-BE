package com.ioes.photo.domain.share.dto;

/**
 * 공유 링크 OG 미리보기에 노출할 스팟 정보.
 *
 * @param name     스팟 이름
 * @param comment  한 줄 코멘트 (없을 수 있음)
 * @param imageUrl 대표 이미지 URL (없을 수 있음)
 * @author 김성민
 */
public record ShareView(
    String name,
    String comment,
    String imageUrl
) {
}
