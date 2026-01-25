package com.dls.driverlicensescannerapi.service;

import com.dls.driverlicensescannerapi.config.ScanProperties;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageValidationService {

    private final ScanProperties scanProperties;

    public ImageValidationService(ScanProperties scanProperties) {
        this.scanProperties = scanProperties;
    }

    public Optional<String> validate(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return Optional.of("Image is required");
        }

        if (!isAllowedImage(image.getContentType(), image.getOriginalFilename())) {
            return Optional.of("Unsupported image type. Allowed: jpg, jpeg, png, webp");
        }

        long size = image.getSize();
        if (size > scanProperties.getMaxFileBytes()) {
            return Optional.of(sizeLimitMessage());
        }

        if (!hasValidMagicBytes(image)) {
            return Optional.of("Invalid image content. Allowed: jpg, jpeg, png, webp");
        }

        return Optional.empty();
    }

    private boolean isAllowedImage(String contentType, String filename) {
        if (StringUtils.hasText(contentType)) {
            String normalizedContentType = normalizeContentType(contentType);
            return normalizedContentType != null
                    && scanProperties.getAllowedMediaTypes().contains(normalizedContentType);
        }
        return isAllowedExtension(filename);
    }

    private String normalizeContentType(String contentType) {
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return (mediaType.getType() + "/" + mediaType.getSubtype()).toLowerCase(Locale.ROOT);
        } catch (InvalidMediaTypeException ex) {
            return null;
        }
    }

    private boolean isAllowedExtension(String filename) {
        String extension = StringUtils.getFilenameExtension(filename);
        if (!StringUtils.hasText(extension)) {
            return false;
        }
        String normalized = "." + extension.toLowerCase(Locale.ROOT);
        return scanProperties.getAllowedExtensions().contains(normalized);
    }

    private boolean hasValidMagicBytes(MultipartFile image) {
        byte[] header = readHeader(image);
        return isJpeg(header) || isPng(header) || isWebp(header);
    }

    private byte[] readHeader(MultipartFile image) {
        try (InputStream inputStream = image.getInputStream()) {
            byte[] buffer = new byte[12];
            int read = inputStream.read(buffer);
            if (read <= 0) {
                return new byte[0];
            }
            return Arrays.copyOf(buffer, read);
        } catch (IOException ex) {
            return new byte[0];
        }
    }

    private boolean isJpeg(byte[] header) {
        return header.length >= 2
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8;
    }

    private boolean isPng(byte[] header) {
        return header.length >= 8
                && (header[0] & 0xFF) == 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A;
    }

    private boolean isWebp(byte[] header) {
        return header.length >= 12
                && header[0] == 0x52
                && header[1] == 0x49
                && header[2] == 0x46
                && header[3] == 0x46
                && header[8] == 0x57
                && header[9] == 0x45
                && header[10] == 0x42
                && header[11] == 0x50;
    }

    private String sizeLimitMessage() {
        long maxBytes = scanProperties.getMaxFileBytes();
        long mb = 1024L * 1024L;
        if (maxBytes < mb) {
            return "Image must be no larger than " + maxBytes + " bytes";
        }
        long maxMb = (maxBytes + mb - 1) / mb;
        return "Image must be no larger than " + maxMb + "MB";
    }
}
