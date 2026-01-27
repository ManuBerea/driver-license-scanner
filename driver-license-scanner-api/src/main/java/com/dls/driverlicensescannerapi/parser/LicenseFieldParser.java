package com.dls.driverlicensescannerapi.parser;

import com.dls.driverlicensescannerapi.dto.LicenseFields;
import com.dls.driverlicensescannerapi.ocr.OcrLine;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ArrayList;

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

        String dateOfBirth = DateParser.findFirstDate(labelIndex.labelRange("3", normalizedLines), true);
        String expiryDate = DateParser.findFirstDate(labelIndex.labelRange("4b", normalizedLines), false);
        String licenceNumber = normalizeLicenseNumber(labelIndex.valueFor("5"));
        if (licenceNumber == null) {
            licenceNumber = normalizeLicenseNumberFromRange(labelIndex.labelRange("5", normalizedLines));
        }

        String addressLine = AddressAssembler.assemble(labelIndex.labelRange("8", normalizedLines));
        List<String> categories = CategoryParser.parse(labelIndex.labelRange("9", normalizedLines));

        return new LicenseFields(
                nullIfBlank(firstName),
                nullIfBlank(lastName),
                dateOfBirth,
                nullIfBlank(addressLine),
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

    private static String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String normalizeLicenseNumber(Optional<String> value) {
        if (value.isEmpty()) {
            return null;
        }
        String cleaned = value.get().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        return nullIfBlank(cleaned);
    }

    private static String normalizeLicenseNumberFromRange(List<String> range) {
        if (range.isEmpty()) {
            return null;
        }
        String combined = normalize(String.join("", range));
        String cleaned = combined.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
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
            if (!normalized.isBlank()) {
                parts.add(normalized);
            }
        }
        String combined = normalize(String.join(" ", parts));
        return combined.isBlank() ? null : combined;
    }
}
