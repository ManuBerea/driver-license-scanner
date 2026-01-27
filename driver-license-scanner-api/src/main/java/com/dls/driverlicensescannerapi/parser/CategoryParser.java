package com.dls.driverlicensescannerapi.parser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class CategoryParser {
    private static final Set<String> EU_CODES = Set.of(
            "AM", "A1", "A2", "A",
            "B1", "B", "BE",
            "C1", "C1E", "C", "CE",
            "D1", "D1E", "D", "DE",
            "G", "H", "M"
    );
    private static final Set<String> NATIONAL_CODES = Set.of("f", "k", "l", "n", "p", "q");

    private CategoryParser() {}

    static List<String> parse(List<String> lines) {
        if (lines.isEmpty()) {
            return List.of();
        }
        String combined = String.join(" ", lines);
        return parse(combined);
    }

    static List<String> parse(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String cleaned = text.replaceAll("\\s+", "")
                .replaceAll("[^A-Za-z0-9/]", "");
        if (cleaned.isBlank()) {
            return List.of();
        }
        String[] tokens = cleaned.split("/");
        Set<String> categories = new LinkedHashSet<>();
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            String upper = token.toUpperCase(Locale.ROOT);
            if (EU_CODES.contains(upper)) {
                categories.add(upper);
                continue;
            }
            String lower = token.toLowerCase(Locale.ROOT);
            if (NATIONAL_CODES.contains(lower)) {
                categories.add(lower);
            }
        }
        return categories.isEmpty() ? List.of() : new ArrayList<>(categories);
    }
}
