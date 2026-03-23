package com.ioes.photo.global.common.util;

import com.ioes.photo.global.config.file.properties.FileProperties;
import com.ioes.photo.global.error.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link FileService} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("FileService 테스트")
class FileServiceTest {

    @TempDir
    Path tempDir;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        FileProperties props = new FileProperties(tempDir.toString(), 1048576L, "jpg,jpeg,png,gif");
        fileService = new FileService(props);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("파일 저장 후 UUID 파일명 반환, 실제 파일 생성 확인")
        void save_createsFile() {
            byte[] content = "hello".getBytes();
            MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", content);

            String savedName = fileService.save(file);

            assertThat(Files.exists(tempDir.resolve(savedName))).isTrue();
            assertThat(savedName).endsWith(".txt");
        }

        @Test
        @DisplayName("빈 파일이면 BusinessException 발생")
        void save_emptyFile_throwsException() {
            MockMultipartFile empty = new MockMultipartFile("file", new byte[0]);
            assertThatThrownBy(() -> fileService.save(empty))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("크기 제한 초과 시 BusinessException 발생")
        void save_oversized_throwsException() {
            MockMultipartFile bigFile = new MockMultipartFile("file", "big.txt", "text/plain", new byte[2000000]);
            assertThatThrownBy(() -> fileService.save(bigFile, 1000L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("saveImage")
    class SaveImage {

        @Test
        @DisplayName("허용 확장자 이미지 저장 성공")
        void saveImage_validExtension() {
            MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[100]);
            String savedName = fileService.saveImage(file);
            assertThat(savedName).endsWith(".jpg");
            assertThat(Files.exists(tempDir.resolve(savedName))).isTrue();
        }

        @Test
        @DisplayName("허용되지 않는 확장자 시 BusinessException 발생")
        void saveImage_invalidExtension_throwsException() {
            MockMultipartFile pdf = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[100]);
            assertThatThrownBy(() -> fileService.saveImage(pdf))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미지");
        }

        @Test
        @DisplayName("이미지 크기 초과 시 BusinessException 발생")
        void saveImage_oversized_throwsException() {
            MockMultipartFile big = new MockMultipartFile("file", "big.jpg", "image/jpeg", new byte[2000000]);
            assertThatThrownBy(() -> fileService.saveImage(big))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("최대");
        }
    }

    @Nested
    @DisplayName("saveAndGetPath")
    class SaveAndGetPath {

        @Test
        @DisplayName("저장된 Path를 반환")
        void saveAndGetPath_returnsPath() {
            MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", new byte[50]);
            Path savedPath = fileService.saveAndGetPath(file);
            assertThat(Files.exists(savedPath)).isTrue();
            assertThat(savedPath.getFileName().toString()).endsWith(".png");
        }
    }
}