package com.ioes.photo.domain.storage.error;

import com.ioes.photo.global.error.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 이미지 저장소 도메인 에러 코드
 *
 * ST001: 보관함 이미지 없음 (404)
 * ST002: 보관함 이미지 이미 등록됨 (409)
 * ST003: 지원하지 않는 이미지 포맷 (400)
 * ST004: 썸네일 생성 실패 (500)
 *
 * @author 황제연
 */
@Getter
@RequiredArgsConstructor
public enum StorageErrorCode implements ErrorCode {

    STORAGE_IMAGE_NOT_FOUND("ST001", "보관함 이미지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    STORAGE_IMAGE_ALREADY_EXISTS("ST002", "보관함 이미지가 이미 등록되어 있습니다.", HttpStatus.CONFLICT),
    UNSUPPORTED_IMAGE_FORMAT("ST003", "지원하지 않는 이미지 포맷입니다.", HttpStatus.BAD_REQUEST),
    THUMBNAIL_GENERATION_FAILED("ST004", "썸네일 생성에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;
}