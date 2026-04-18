package com.ioes.photo.global.common.util;

import com.ioes.photo.global.config.file.properties.FileProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

/**
 * 파일 저장 서비스 컴포넌트
 * FileProperties로 주입받은 설정(업로드 경로, 최대 크기, 허용 확장자)을 기반으로 파일을 저장합니다
 * 클라우드 환경이나 파일 저장 방식 논의 후, 변경될 수도 있음.
 *
 *
 * @see FileUtils
 * @see FileProperties
 * @author 황제연
 */
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileProperties fileProperties;

    public String save(MultipartFile file) {
        return save(file, fileProperties.uploadPath());
    }

    public String save(MultipartFile file, Path directory) {
        FileUtils.validateNotEmpty(file);
        String savedName = FileUtils.generateFileName(file.getOriginalFilename());
        FileUtils.saveToPath(file, directory, savedName);
        return savedName;
    }

    public String save(MultipartFile file, long maxBytes) {
        FileUtils.validateNotEmpty(file);
        FileUtils.validateSize(file, maxBytes);
        String savedName = FileUtils.generateFileName(file.getOriginalFilename());
        FileUtils.saveToPath(file, fileProperties.uploadPath(), savedName);
        return savedName;
    }

    public Path saveAndGetPath(MultipartFile file) {
        FileUtils.validateNotEmpty(file);
        String savedName = FileUtils.generateFileName(file.getOriginalFilename());
        return FileUtils.saveToPath(file, fileProperties.uploadPath(), savedName);
    }

    public String saveImage(MultipartFile file) {
        FileUtils.validateNotEmpty(file);
        FileUtils.validateImage(file, fileProperties.imageExtensionSet());
        FileUtils.validateSize(file, fileProperties.maxSize());
        String savedName = FileUtils.generateFileName(file.getOriginalFilename());
        FileUtils.saveToPath(file, fileProperties.uploadPath(), savedName);
        return savedName;
    }

    public String saveImage(MultipartFile file, Path directory) {
        FileUtils.validateNotEmpty(file);
        FileUtils.validateImage(file, fileProperties.imageExtensionSet());
        FileUtils.validateSize(file, fileProperties.maxSize());
        String savedName = FileUtils.generateFileName(file.getOriginalFilename());
        FileUtils.saveToPath(file, directory, savedName);
        return savedName;
    }
}