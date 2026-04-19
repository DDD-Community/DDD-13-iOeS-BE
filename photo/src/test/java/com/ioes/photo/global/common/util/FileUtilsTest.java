package com.ioes.photo.global.common.util;

import com.ioes.photo.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link FileUtils} 단위 테스트 (static 헬퍼 메서드).
 *
 * @author 황제연
 */
@DisplayName("FileUtils 테스트")
class FileUtilsTest {

    @Nested
    @DisplayName("getExtension")
    class GetExtension {

        @Test
        @DisplayName("일반 파일명에서 확장자 추출 (소문자 변환)")
        void returnsLowercaseExtension() {
            assertThat(FileUtils.getExtension("photo.JPG")).isEqualTo("jpg");
            assertThat(FileUtils.getExtension("image.PNG")).isEqualTo("png");
        }

        @Test
        @DisplayName("확장자가 없으면 빈 문자열")
        void returnsEmpty_noExtension() {
            assertThat(FileUtils.getExtension("filename")).isEmpty();
        }

        @Test
        @DisplayName("null이면 빈 문자열")
        void returnsEmpty_null() {
            assertThat(FileUtils.getExtension((String) null)).isEmpty();
        }

        @Test
        @DisplayName("점으로 끝나는 파일명은 빈 문자열")
        void returnsEmpty_endsWithDot() {
            assertThat(FileUtils.getExtension("file.")).isEmpty();
        }

        @Test
        @DisplayName("Path에서 확장자 추출")
        void fromPath() {
            Path path = Path.of("uploads/image.jpeg");
            assertThat(FileUtils.getExtension(path)).isEqualTo("jpeg");
        }
    }

    @Nested
    @DisplayName("getBaseName")
    class GetBaseName {

        @Test
        @DisplayName("확장자 제외한 기본 이름 반환")
        void returnsBaseName() {
            assertThat(FileUtils.getBaseName("photo.jpg")).isEqualTo("photo");
        }

        @Test
        @DisplayName("확장자 없으면 전체 파일명 반환")
        void returnsFullName_noExtension() {
            assertThat(FileUtils.getBaseName("photofile")).isEqualTo("photofile");
        }

        @Test
        @DisplayName("null이면 빈 문자열")
        void returnsEmpty_null() {
            assertThat(FileUtils.getBaseName(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("generateFileName")
    class GenerateFileName {

        @Test
        @DisplayName("UUID + 원본 확장자 조합의 파일명 생성")
        void generatesUniqueNameWithExtension() {
            String name = FileUtils.generateFileName("photo.jpg");
            assertThat(name).endsWith(".jpg");
            assertThat(name).hasSize(36 + 1 + 3); // uuid(36) + "." + "jpg"
        }

        @Test
        @DisplayName("확장자 없는 파일명은 UUID만 반환")
        void generatesUuidOnly_noExtension() {
            String name = FileUtils.generateFileName("noextfile");
            assertThat(name).hasSize(36);
        }

        @Test
        @DisplayName("두 번 호출 시 서로 다른 이름 생성 (UUID 기반)")
        void generatesUniqueName() {
            String name1 = FileUtils.generateFileName("a.jpg");
            String name2 = FileUtils.generateFileName("a.jpg");
            assertThat(name1).isNotEqualTo(name2);
        }
    }

    @Nested
    @DisplayName("formatSize")
    class FormatSize {

        @Test
        @DisplayName("1024 미만은 Bytes 단위")
        void formatsBytes() {
            assertThat(FileUtils.formatSize(512)).isEqualTo("512 B");
        }

        @Test
        @DisplayName("1MB 표현")
        void formatsMegabytes() {
            assertThat(FileUtils.formatSize(1048576L)).isEqualTo("1.00 MB");
        }

        @Test
        @DisplayName("1KB 표현")
        void formatsKilobytes() {
            assertThat(FileUtils.formatSize(1024L)).isEqualTo("1.00 KB");
        }
    }

    @Nested
    @DisplayName("isImage")
    class IsImage {

        private final Set<String> allowedExts = Set.of("jpg", "jpeg", "png", "gif");

        @Test
        @DisplayName("허용된 확장자 파일이면 true")
        void returnsTrue_allowedExtension() {
            MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[10]);
            assertThat(FileUtils.isImage(file, allowedExts)).isTrue();
        }

        @Test
        @DisplayName("허용되지 않은 확장자 파일이면 false")
        void returnsFalse_disallowedExtension() {
            MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[10]);
            assertThat(FileUtils.isImage(file, allowedExts)).isFalse();
        }

        @Test
        @DisplayName("파일명 String으로도 확인 가능")
        void byFilename() {
            assertThat(FileUtils.isImage("image.png", allowedExts)).isTrue();
            assertThat(FileUtils.isImage("document.pdf", allowedExts)).isFalse();
        }
    }

    @Nested
    @DisplayName("validateNotEmpty")
    class ValidateNotEmpty {

        @Test
        @DisplayName("빈 파일이면 BusinessException 발생")
        void throwsException_emptyFile() {
            MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);
            assertThatThrownBy(() -> FileUtils.validateNotEmpty(emptyFile))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("null이면 BusinessException 발생")
        void throwsException_null() {
            assertThatThrownBy(() -> FileUtils.validateNotEmpty(null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("내용 있는 파일은 예외 없음")
        void noException_validFile() {
            MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[100]);
            FileUtils.validateNotEmpty(file);
        }
    }

    @Nested
    @DisplayName("validateSize")
    class ValidateSize {

        @Test
        @DisplayName("최대 크기 초과 시 BusinessException 발생")
        void throwsException_oversized() {
            MockMultipartFile file = new MockMultipartFile("file", "big.jpg", "image/jpeg", new byte[1001]);
            assertThatThrownBy(() -> FileUtils.validateSize(file, 1000L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("최대");
        }

        @Test
        @DisplayName("최대 크기 이하는 예외 없음")
        void noException_withinLimit() {
            MockMultipartFile file = new MockMultipartFile("file", "small.jpg", "image/jpeg", new byte[500]);
            FileUtils.validateSize(file, 1000L);
        }
    }

    @Nested
    @DisplayName("validateImage")
    class ValidateImage {

        private final Set<String> allowedExts = Set.of("jpg", "jpeg", "png");

        @Test
        @DisplayName("허용 확장자가 아니면 BusinessException 발생")
        void throwsException_invalidExtension() {
            MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[10]);
            assertThatThrownBy(() -> FileUtils.validateImage(file, allowedExts))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미지");
        }

        @Test
        @DisplayName("허용 확장자면 예외 없음")
        void noException_validImage() {
            MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[10]);
            FileUtils.validateImage(file, allowedExts);
        }
    }
}
