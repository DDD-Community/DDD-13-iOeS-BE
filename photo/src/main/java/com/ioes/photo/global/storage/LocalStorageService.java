package com.ioes.photo.global.storage;

import com.ioes.photo.global.common.util.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 로컬 파일 시스템 기반 저장소 구현체.
 *
 * AWS S3 연동 전까지 임시로 사용하는 로컬 저장소입니다.
 *
 * TODO: S3StorageService로 교체 예정
 *
 * @author 황제연
 */
@Service
@RequiredArgsConstructor
public class LocalStorageService implements StorageService {

    private final FileService fileService;

    @Override
    public String uploadImage(MultipartFile file) {
        String savedName = fileService.saveImage(file);
        return "/uploads/" + savedName;
    }
}