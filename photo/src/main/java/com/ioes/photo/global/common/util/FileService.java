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

    /**
     * 기본 업로드 디렉터리에 저장합니다.
     *
     * @param file 저장할 파일
     * @return UUID 기반 저장 파일명
     */
    public String save(MultipartFile file) {
        return save(file, fileProperties.uploadPath());
    }

    /**
     * 지정 디렉터리에 저장합니다.
     *
     * @param file      저장할 파일
     * @param directory 저장할 디렉터리
     * @return UUID 기반 저장 파일명
     */
    public String save(MultipartFile file, Path directory) {
        FileUtils.validateNotEmpty(file);
        String savedName = FileUtils.generateFileName(file.getOriginalFilename());
        FileUtils.saveToPath(file, directory, savedName);
        return savedName;
    }

    /**
     * 최대 크기 검증 후 기본 경로에 저장합니다.
     *
     * @param file     저장할 파일
     * @param maxBytes 최대 허용 파일 크기 (bytes)
     * @return UUID 기반 저장 파일명
     */
    public String save(MultipartFile file, long maxBytes) {
        FileUtils.validateNotEmpty(file);
        FileUtils.validateSize(file, maxBytes);
        String savedName = FileUtils.generateFileName(file.getOriginalFilename());
        FileUtils.saveToPath(file, fileProperties.uploadPath(), savedName);
        return savedName;
    }

    /**
     * 기본 업로드 디렉터리에 저장하고 Path를 반환합니다.
     *
     * @param file 저장할 파일
     * @return 저장된 파일의 Path
     */
    public Path saveAndGetPath(MultipartFile file) {
        FileUtils.validateNotEmpty(file);
        String savedName = FileUtils.generateFileName(file.getOriginalFilename());
        return FileUtils.saveToPath(file, fileProperties.uploadPath(), savedName);
    }

    /**
     * 이미지 검증(확장자 + 최대 크기)후 기본 디렉터리에 저장합니다.
     *
     * @param file 저장할 이미지 파일
     * @return UUID 기반 저장 파일명
     */
    public String saveImage(MultipartFile file) {
        FileUtils.validateNotEmpty(file);
        FileUtils.validateImage(file, fileProperties.imageExtensionSet());
        FileUtils.validateSize(file, fileProperties.maxSize());
        String savedName = FileUtils.generateFileName(file.getOriginalFilename());
        FileUtils.saveToPath(file, fileProperties.uploadPath(), savedName);
        return savedName;
    }

    /**
     * 이미지 검증 후 지정 디렉터리에 저장합니다.
     *
     * @param file      저장할 이미지 파일
     * @param directory 저장할 디렉터리
     * @return UUID 기반 저장 파일명
     */
    public String saveImage(MultipartFile file, Path directory) {
        FileUtils.validateNotEmpty(file);
        FileUtils.validateImage(file, fileProperties.imageExtensionSet());
        FileUtils.validateSize(file, fileProperties.maxSize());
        String savedName = FileUtils.generateFileName(file.getOriginalFilename());
        FileUtils.saveToPath(file, directory, savedName);
        return savedName;
    }
}