package com.dls.driverlicensescannerapi.config;

import jakarta.validation.constraints.Min;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "scan")
public class ScanProperties {
    private static final Set<String> DEFAULT_MEDIA_TYPES = Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");
    private static final Set<String> DEFAULT_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");

    /** Max allowed upload size in bytes. */
    @Min(1)
    private long maxFileBytes = 10L * 1024 * 1024;

    /** Allowed content types (lowercase). */
    private Set<String> allowedMediaTypes = new LinkedHashSet<>(DEFAULT_MEDIA_TYPES);

    /** Allowed filename extensions (lowercase, include dot). */
    private Set<String> allowedExtensions = new LinkedHashSet<>(DEFAULT_EXTENSIONS);

    public void setAllowedMediaTypes(Set<String> allowedMediaTypes) {
        this.allowedMediaTypes = normalizeMediaTypes(allowedMediaTypes);
    }

    public void setAllowedExtensions(Set<String> allowedExtensions) {
        this.allowedExtensions = normalizeExtensions(allowedExtensions);
    }

    private static Set<String> normalizeMediaTypes(Collection<String> values) {
        if (values == null) {
            return new LinkedHashSet<>(DEFAULT_MEDIA_TYPES);
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            normalized.add(value.trim().toLowerCase(Locale.ROOT));
        }
        return normalized;
    }

    private static Set<String> normalizeExtensions(Collection<String> values) {
        if (values == null) {
            return new LinkedHashSet<>(DEFAULT_EXTENSIONS);
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            String trimmed = value.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.startsWith(".")) {
                trimmed = "." + trimmed;
            }
            if (trimmed.length() > 1) {
                normalized.add(trimmed);
            }
        }
        return normalized;
    }
}
