package com.dls.driverlicensescannerapi.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

final class AddressAssembler {
    private static final Pattern POSTCODE_COMPACT_PATTERN =
            Pattern.compile("^[A-Z]{1,2}\\d[A-Z\\d]?\\d[A-Z]{2}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MONTH_STAMP_PATTERN =
            Pattern.compile("^(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|SEPT|OCT|NOV|DEC)\\s?\\d{2}$",
                    Pattern.CASE_INSENSITIVE);
    private static final List<String> ADDRESS_SUFFIXES =
            List.of("ROAD", "STREET", "AVENUE", "CRESCENT", "CLOSE", "LANE", "DRIVE",
                    "WAY", "PLACE", "COURT", "GROVE", "PARK", "SQUARE", "HILL", "TERRACE", "GARDENS");

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
            if (normalized.length() == 1 && Character.isLetter(normalized.charAt(0))) {
                continue;
            }
            if (MONTH_STAMP_PATTERN.matcher(normalized).matches()) {
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
        cleaned = cleaned.replaceAll("(?<=\\d)(?=[A-Z])", " ");
        cleaned = cleaned.replaceAll("(?<=[A-Z])(?=\\d)", " ");
        cleaned = splitSuffix(cleaned);
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

    private static String splitSuffix(String text) {
        String upper = text.toUpperCase(Locale.ROOT);
        for (String suffix : ADDRESS_SUFFIXES) {
            if (upper.endsWith(suffix) && upper.length() > suffix.length()) {
                int splitIndex = upper.length() - suffix.length();
                if (splitIndex > 0 && upper.charAt(splitIndex - 1) != ' ') {
                    return text.substring(0, splitIndex) + " " + text.substring(splitIndex);
                }
            }
        }
        return text;
    }
}
