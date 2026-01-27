package com.dls.driverlicensescannerapi.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

final class AddressAssembler {
    private static final Pattern POSTCODE_COMPACT_PATTERN =
            Pattern.compile("^[A-Z]{1,2}\\d[A-Z\\d]?\\d[A-Z]{2}$", Pattern.CASE_INSENSITIVE);

    private AddressAssembler() {}

    static String assemble(List<String> lines) {
        if (lines.isEmpty()) {
            return null;
        }
        List<String> cleaned = new ArrayList<>();
        for (String line : lines) {
            String normalized = normalize(line);
            if (normalized.isBlank()) {
                continue;
            }
            cleaned.add(normalizeAddressToken(normalized));
        }
        String combined = String.join(", ", cleaned).trim();
        return combined.isBlank() ? null : combined;
    }

    private static String normalizeAddressToken(String token) {
        String cleaned = token.replaceAll("\\s+", " ").trim();
        cleaned = cleaned.replaceAll(",\\s*", ", ");
        cleaned = cleaned.replaceAll("\\.(?=\\S)", ". ");
        String compact = cleaned.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        if (POSTCODE_COMPACT_PATTERN.matcher(compact).matches()) {
            return formatPostcode(compact);
        }
        return cleaned;
    }

    private static String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private static String formatPostcode(String raw) {
        String upper = raw.toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        if (upper.length() <= 3) {
            return upper;
        }
        String outward = upper.substring(0, upper.length() - 3);
        String inward = upper.substring(upper.length() - 3);
        return (outward + " " + inward).trim();
    }
}
