package com.dls.driverlicensescannerapi.eval;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

public final class FileMultipartFile implements MultipartFile {

    @Getter
    private final Path path;
    private final byte[] content;
    private final String filename;
    private final String contentType;

    public FileMultipartFile(Path path) throws IOException {
        this.path = path;
        this.content = Files.readAllBytes(path);
        this.filename = path.getFileName().toString();
        this.contentType = detectContentType(filename);
    }

    @Override
    public String getName() {
        return "image";
    }

    @Override
    public String getOriginalFilename() {
        return filename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() {
        return content.clone();
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(java.io.File dest) throws IOException {
        Files.write(dest.toPath(), content);
    }

    private static String detectContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }
}
