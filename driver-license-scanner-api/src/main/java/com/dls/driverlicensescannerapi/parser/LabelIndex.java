package com.dls.driverlicensescannerapi.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LabelIndex {
    private static final Pattern LABEL_PATTERN =
            Pattern.compile("^(?<label>(?:1|2|3|4a|4b|5|7|8|9))(?=\\s|[\\.)]|$)\\s*[\\.)]?\\s*(?<value>.*)$",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern INLINE_LABEL_PATTERN =
            Pattern.compile("(?<![A-Z0-9])(?<label>(?:1|2|3|4a|4b|5|7|8|9))(?=\\s|[\\.)]|$)",
                    Pattern.CASE_INSENSITIVE);

    private final List<LabelMatch> matches;

    private LabelIndex(List<LabelMatch> matches) {
        this.matches = matches;
    }

    static LabelIndex from(List<String> lines) {
        List<LabelMatch> matches = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                Matcher matcher = LABEL_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String label = matcher.group("label").toLowerCase(Locale.ROOT);
                    String value = lineNormalize(matcher.group("value"));
                    matches.add(new LabelMatch(label, value, i));
                } else {
                    Matcher inline = INLINE_LABEL_PATTERN.matcher(line);
                    if (inline.find()) {
                        String label = inline.group("label").toLowerCase(Locale.ROOT);
                        String value = lineNormalize(line.substring(inline.end())
                                .replaceFirst("^[\\.)\\s]+", ""));
                        matches.add(new LabelMatch(label, value, i));
                    }
                }
            }
        return new LabelIndex(matches);
    }

    Optional<String> valueFor(String label) {
        return matches.stream()
                .filter(match -> match.label().equals(label))
                .findFirst()
                .map(LabelMatch::value);
    }

    Optional<LabelMatch> labelMatch(String label) {
        return matches.stream()
                .filter(match -> match.label().equals(label))
                .findFirst();
    }

    Optional<Integer> nextLabelIndexAfter(int index) {
        return matches.stream()
                .map(LabelMatch::index)
                .filter(matchIndex -> matchIndex > index)
                .sorted()
                .findFirst();
    }

    List<String> labelRange(String label, List<String> lines) {
        Optional<LabelMatch> match = labelMatch(label);
        if (match.isEmpty()) {
            return List.of();
        }
        int startIndex = match.get().index();
        int endIndex = nextLabelIndexAfter(startIndex).orElse(lines.size());
        List<String> range = new ArrayList<>();
        if (!match.get().value().isBlank()) {
            range.add(match.get().value());
        }
        for (int i = startIndex + 1; i < endIndex; i++) {
            String line = lines.get(i);
            if (!line.isBlank() && !isLabelLine(line)) {
                range.add(line);
            }
        }
        return range;
    }

    static boolean isLabelLine(String line) {
        return LABEL_PATTERN.matcher(line).matches();
    }

    private static String lineNormalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    record LabelMatch(String label, String value, int index) {}
}
