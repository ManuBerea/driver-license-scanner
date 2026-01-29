package com.dls.driverlicensescannerapi.parser;

import com.dls.driverlicensescannerapi.dto.LicenseFields;
import com.dls.driverlicensescannerapi.ocr.OcrLine;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class LicenseFieldParser {

    private LicenseFieldParser() {}

    public static LicenseFields parse(List<OcrLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return emptyFields();
        }

        List<String> normalizedLines = new ArrayList<>();
        for (OcrLine line : lines) {
            if (line == null || line.text() == null) {
                continue;
            }
            String normalized = normalize(line.text());
            if (!normalized.isBlank()) {
                normalizedLines.add(normalized);
            }
        }

        if (normalizedLines.isEmpty()) {
            return emptyFields();
        }

        LabelIndex labelIndex = LabelIndex.from(normalizedLines);

        String lastName = extractLabelText(labelIndex, "1", normalizedLines);
        String firstName = extractLabelText(labelIndex, "2", normalizedLines);
        if (lastName == null) {
            lastName = fallbackFromPreviousLine(labelIndex, "2", normalizedLines);
        }

        String dateOfBirth = DateParser.findFirstDate(labelIndex.labelRange("3", normalizedLines), true);
        if (dateOfBirth == null && labelIndex.labelMatch("3").isEmpty()) {
            dateOfBirth = inferDobBetweenLabels(labelIndex, normalizedLines);
        }
        String expiryDate = DateParser.findFirstDate(labelIndex.labelRange("4b", normalizedLines), false);
        String licenceNumber = normalizeLicenseNumber(labelIndex.valueFor("5"));
        if (licenceNumber == null) {
            licenceNumber = normalizeLicenseNumberFromRange(labelIndex.labelRange("5", normalizedLines));
        }

        String addressLine = AddressAssembler.assemble(labelIndex.labelRange("8", normalizedLines));
        List<String> categories = CategoryParser.parse(labelIndex.labelRange("9", normalizedLines));

        return new LicenseFields(
                nullIfBlank(sanitizeValue(firstName)),
                nullIfBlank(sanitizeValue(lastName)),
                dateOfBirth,
                nullIfBlank(sanitizeValue(addressLine)),
                nullIfBlank(licenceNumber),
                expiryDate,
                categories
        );
    }

    private static LicenseFields emptyFields() {
        return new LicenseFields(null, null, null, null, null, null, List.of());
    }

    private static String normalize(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    private static String sanitizeValue(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = value.replaceAll("[^\\p{L}\\p{N},\\.\\s]", "");
        return normalize(sanitized);
    }

    private static String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String normalizeLicenseNumber(Optional<String> value) {
        if (value.isEmpty()) {
            return null;
        }
        String cleaned = value.get().replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        return nullIfBlank(cleaned);
    }

    private static String normalizeLicenseNumberFromRange(List<String> range) {
        if (range.isEmpty()) {
            return null;
        }
        String best = null;
        for (String line : range) {
            if (line == null || line.isBlank()) {
                continue;
            }
            String[] tokens = line.split("[^A-Za-z0-9]+");
            for (String token : tokens) {
                if (token.isBlank()) {
                    continue;
                }
                if (best == null || token.length() > best.length()) {
                    best = token;
                }
            }
        }
        if (best == null) {
            return null;
        }
        String cleaned = best.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        return nullIfBlank(cleaned);
    }
    private static String extractLabelText(LabelIndex labelIndex, String label, List<String> lines) {
        List<String> range = labelIndex.labelRange(label, lines);
        if (range.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (String line : range) {
            String normalized = normalize(line);
            if (normalized.isBlank()) {
                continue;
            }
            if ("1".equals(label) || "2".equals(label)) {
                if (DateParser.containsDate(normalized)) {
                    break;
                }
                if (normalized.length() == 1) {
                    continue;
                }
                if (normalized.matches("^\\d+$")) {
                    continue;
                }
            }
            parts.add(normalized);
        }
        String combined = normalize(String.join(" ", parts));
        return combined.isBlank() ? null : combined;
    }

    private static String fallbackFromPreviousLine(LabelIndex labelIndex, String label, List<String> lines) {
        Optional<LabelIndex.LabelMatch> match = labelIndex.labelMatch(label);
        if (match.isEmpty()) {
            return null;
        }
        int index = match.get().index() - 1;
        while (index >= 0) {
            String line = normalize(lines.get(index));
            if (line.isBlank() || LabelIndex.isLabelLine(line)) {
                index--;
                continue;
            }
            if (DateParser.containsDate(line)) {
                index--;
                continue;
            }
            if (line.matches("^\\d+$")) {
                index--;
                continue;
            }
            return line;
        }
        return null;
    }

    private static String inferDobBetweenLabels(LabelIndex labelIndex, List<String> lines) {
        Optional<LabelIndex.LabelMatch> labelTwo = labelIndex.labelMatch("2");
        if (labelTwo.isEmpty()) {
            return null;
        }
        int startIndex = labelTwo.get().index() + 1;
        int endIndex = Integer.MAX_VALUE;

        Optional<LabelIndex.LabelMatch> label4a = labelIndex.labelMatch("4a");
        if (label4a.isPresent() && label4a.get().index() > labelTwo.get().index()) {
            endIndex = Math.min(endIndex, label4a.get().index());
        }
        Optional<LabelIndex.LabelMatch> label4b = labelIndex.labelMatch("4b");
        if (label4b.isPresent() && label4b.get().index() > labelTwo.get().index()) {
            endIndex = Math.min(endIndex, label4b.get().index());
        }

        if (endIndex == Integer.MAX_VALUE) {
            return null;
        }

        String found = null;
        for (int i = startIndex; i < endIndex && i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank() || LabelIndex.isLabelLine(line)) {
                continue;
            }
            String date = DateParser.parseDate(Optional.of(line), true);
            if (date != null) {
                if (found != null) {
                    return null;
                }
                found = date;
            }
        }
        return found;
    }
}
