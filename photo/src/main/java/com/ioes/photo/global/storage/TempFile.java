package com.ioes.photo.global.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 외부 프로세스(ImageMagick, libvips 등)에 전달할 임시 파일을 생성하고,
 * close() 시 자동으로 삭제한다.
 *
 * @author 황제연
 */
final class TempFile implements AutoCloseable {

    final Path path;

    TempFile(String prefix, String suffix) throws IOException {
        this.path = Files.createTempFile(prefix, suffix);
    }

    @Override
    public void close() {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
