package com.ioes.photo.global.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 저장소 인터페이스.
 *
 * @author 황제연
 */
public interface StorageService {
    String uploadImage(MultipartFile file);
}