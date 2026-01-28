package com.dls.driverlicensescannerapi.eval;

import com.dls.driverlicensescannerapi.dto.LicenseFields;
import com.dls.driverlicensescannerapi.ocr.OcrClient;
import com.dls.driverlicensescannerapi.ocr.OcrResult;
import com.dls.driverlicensescannerapi.parser.LicenseFieldParser;
import com.dls.driverlicensescannerapi.service.FieldConfidenceCalculator;
import com.dls.driverlicensescannerapi.validator.ValidationService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.restclient.RestTemplateBuilder;
import tools.jackson.databind.ObjectMapper;

public final class EvaluationRunner {

    private static final List<String> REQUIRED_FIELDS = List.of(
            "firstName",
            "lastName",
            "dateOfBirth",
            "addressLine",
            "licenceNumber",
            "expiryDate"
    );

    private static final List<String> ALL_FIELDS = List.of(
            "firstName",
            "lastName",
            "dateOfBirth",
            "addressLine",
            "licenceNumber",
            "expiryDate",
            "categories"
    );

    private static final double CONFIDENCE_THRESHOLD = 0.70;
    private static final double REQUIRED_ACCURACY_THRESHOLD = 0.85;
    private static final long MEDIAN_TIME_THRESHOLD_MS = 10_000;

    private EvaluationRunner() {}

    public static void main(String[] args) throws IOException {
        String datasetDir = envOrDefault("EVAL_DATASET_DIR", "docs/images");
        String reportPath = envOrDefault("EVAL_REPORT_PATH", "reports/ocr_engine_comparison.md");
        String engineList = envOrDefault("EVAL_ENGINES", "paddle,vision");
        boolean enableVision = envBool(System.getenv("ENABLE_VISION_OCR"), false);

        String workerUrl = System.getenv("OCR_WORKER_URL");
        String internalKey = System.getenv("X_INTERNAL_KEY");
        if (workerUrl == null || workerUrl.isBlank() || internalKey == null || internalKey.isBlank()) {
            System.err.println("OCR_WORKER_URL and X_INTERNAL_KEY must be set.");
            System.exit(1);
        }

        List<String> engines = Arrays.stream(engineList.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toList());
        if (!enableVision) {
            engines = engines.stream()
                    .filter(engine -> !"vision".equalsIgnoreCase(engine))
                    .collect(Collectors.toList());
        }

        Path datasetPath = resolveDatasetPath(datasetDir);
        List<GroundTruthSample> samples = loadSamples(datasetPath.toString());
        if (samples.isEmpty()) {
            System.err.println("No samples found in " + datasetPath);
            System.exit(1);
        }

        OcrClient ocrClient = new OcrClient(new RestTemplateBuilder(), workerUrl, internalKey);
        ValidationService validationService = new ValidationService();

        Map<String, Map<String, MetricsAccumulator>> metrics = new LinkedHashMap<>();

        for (String engine : engines) {
            Map<String, MetricsAccumulator> engineMetrics = new LinkedHashMap<>();
            for (String bucket : List.of("clean", "medium", "poor", "all")) {
                engineMetrics.put(bucket, new MetricsAccumulator());
            }

            for (GroundTruthSample sample : samples) {
                OcrResult ocrResult;
                long start = System.nanoTime();
                try {
                    FileMultipartFile file = new FileMultipartFile(sample.imageFile());
                    ocrResult = ocrClient.scan(file, sample.id(), engine);
                } catch (Exception ex) {
                    MetricsAccumulator acc = engineMetrics.get(sample.bucket());
                    acc.recordFailure();
                    continue;
                }
                LicenseFields fields = LicenseFieldParser.parse(ocrResult.lines());
                double confidence = FieldConfidenceCalculator.compute(fields);
                validationService.validate(fields);
                long elapsedMs = (System.nanoTime() - start) / 1_000_000;

                EvaluationResult evaluationResult = evaluate(sample, fields);
                evaluationResult.confidence = confidence;
                evaluationResult.elapsedMs = elapsedMs;

                engineMetrics.get(sample.bucket()).record(evaluationResult);
                engineMetrics.get("all").record(evaluationResult);
            }

            metrics.put(engine, engineMetrics);
        }

        String report = renderReport(metrics, samples);
        writeReport(reportPath, report);
        System.out.println("Report written to " + reportPath);
    }

    private static Path resolveDatasetPath(String datasetDir) {
        Path path = Paths.get(datasetDir);
        if (Files.exists(path)) {
            return path;
        }
        Path fallback = Paths.get("..").resolve(datasetDir);
        if (Files.exists(fallback)) {
            return fallback.normalize();
        }
        return path;
    }

    private static List<GroundTruthSample> loadSamples(String datasetDir) throws IOException {
        Path root = Paths.get(datasetDir);
        Path truthDir = root.resolve("ground_truth");
        if (!Files.exists(truthDir)) {
            return List.of();
        }

        ObjectMapper mapper = new ObjectMapper();
        List<GroundTruthSample> samples = new ArrayList<>();

        try (var paths = Files.list(truthDir)) {
            for (Path path : paths.toList()) {
                if (!path.getFileName().toString().endsWith(".json")) {
                    continue;
                }
                GroundTruth truth = mapper.readValue(path.toFile(), GroundTruth.class);
                String bucket = bucketFromFile(path.getFileName().toString());
                Path imagePath = root.resolve(truth.image());
                if (!Files.exists(imagePath)) {
                    continue;
                }
                samples.add(new GroundTruthSample(bucket, imagePath, truth.fields(), path.getFileName().toString()));
            }
        }

        return samples;
    }

    private static String bucketFromFile(String filename) {
        if (filename.startsWith("clean_")) {
            return "clean";
        }
        if (filename.startsWith("medium_")) {
            return "medium";
        }
        if (filename.startsWith("poor_")) {
            return "poor";
        }
        return "unknown";
    }

    private static EvaluationResult evaluate(GroundTruthSample sample, LicenseFields actual) {
        GroundTruthFields expected = sample.truth();
        EvaluationResult result = new EvaluationResult();

        result.totalSamples = 1;
        boolean allRequiredCorrect = true;
        for (String field : REQUIRED_FIELDS) {
            boolean match = compareField(field, expected, actual);
            result.fieldTotals.merge(field, 1, Integer::sum);
            if (match) {
                result.fieldCorrect.merge(field, 1, Integer::sum);
            } else {
                allRequiredCorrect = false;
                result.failurePatterns.merge(patternFor(field, actual, expected), 1, Integer::sum);
            }
        }

        boolean categoriesMatch = compareField("categories", expected, actual);
        result.fieldTotals.merge("categories", 1, Integer::sum);
        if (categoriesMatch) {
            result.fieldCorrect.merge("categories", 1, Integer::sum);
        } else {
            result.failurePatterns.merge(patternFor("categories", actual, expected), 1, Integer::sum);
        }

        if (allRequiredCorrect) {
            result.requiredAllCorrect = 1;
        }
        return result;
    }

    private static boolean compareField(String field, GroundTruthFields expected, LicenseFields actual) {
        return switch (field) {
            case "firstName" -> equalsNormalized(expected.firstName(), actual.firstName());
            case "lastName" -> equalsNormalized(expected.lastName(), actual.lastName());
            case "dateOfBirth" -> equalsNormalized(expected.dateOfBirth(), actual.dateOfBirth());
            case "addressLine" -> equalsAddress(expected.addressLine(), actual.addressLine());
            case "licenceNumber" -> equalsLicence(expected.licenceNumber(), actual.licenceNumber());
            case "expiryDate" -> equalsNormalized(expected.expiryDate(), actual.expiryDate());
            case "categories" -> equalsCategories(expected.categories(), actual.categories());
            default -> false;
        };
    }

    private static boolean equalsNormalized(String expected, String actual) {
        return normalizeText(expected).equals(normalizeText(actual));
    }

    private static boolean equalsAddress(String expected, String actual) {
        return normalizeAddress(expected).equals(normalizeAddress(actual));
    }

    private static boolean equalsLicence(String expected, String actual) {
        return normalizeLicence(expected).equals(normalizeLicence(actual));
    }

    private static boolean equalsCategories(List<String> expected, List<String> actual) {
        Set<String> expectedSet = normalizeCategories(expected);
        Set<String> actualSet = normalizeCategories(actual);
        return expectedSet.equals(actualSet);
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.UK);
    }

    private static String normalizeAddress(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        normalized = normalized.replaceAll("\\s*,\\s*", ", ");
        return normalized.toUpperCase(Locale.UK);
    }

    private static String normalizeLicence(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.UK);
    }

    private static Set<String> normalizeCategories(List<String> categories) {
        if (categories == null) {
            return Set.of();
        }
        return categories.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.UK))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String patternFor(String field, LicenseFields actual, GroundTruthFields expected) {
        String actualValue = switch (field) {
            case "firstName" -> actual.firstName();
            case "lastName" -> actual.lastName();
            case "dateOfBirth" -> actual.dateOfBirth();
            case "addressLine" -> actual.addressLine();
            case "licenceNumber" -> actual.licenceNumber();
            case "expiryDate" -> actual.expiryDate();
            default -> null;
        };
        String expectedValue = switch (field) {
            case "firstName" -> expected.firstName();
            case "lastName" -> expected.lastName();
            case "dateOfBirth" -> expected.dateOfBirth();
            case "addressLine" -> expected.addressLine();
            case "licenceNumber" -> expected.licenceNumber();
            case "expiryDate" -> expected.expiryDate();
            default -> null;
        };
        if (actualValue == null || actualValue.isBlank()) {
            return "missing_" + field;
        }
        if (expectedValue == null || expectedValue.isBlank()) {
            return "unexpected_" + field;
        }
        return "mismatch_" + field;
    }

    private static String renderReport(Map<String, Map<String, MetricsAccumulator>> metrics, List<GroundTruthSample> samples) {
        StringBuilder builder = new StringBuilder();
        builder.append("# OCR Engine Comparison Report\n\n");
        builder.append("Generated: ").append(Instant.now().toString()).append("\n\n");
        builder.append("Total samples: ").append(samples.size()).append("\n\n");

        for (Map.Entry<String, Map<String, MetricsAccumulator>> entry : metrics.entrySet()) {
            builder.append("## Engine: ").append(entry.getKey()).append("\n\n");
            for (Map.Entry<String, MetricsAccumulator> bucketEntry : entry.getValue().entrySet()) {
                MetricsAccumulator accumulator = bucketEntry.getValue();
                builder.append("### Bucket: ").append(bucketEntry.getKey()).append("\n\n");
                builder.append(accumulator.renderSummary());
                builder.append("\n");
                builder.append(accumulator.renderFieldTable());
                builder.append("\n");
                builder.append(accumulator.renderFailurePatterns());
                builder.append("\n");
            }
        }

        return builder.toString();
    }

    private static void writeReport(String path, String content) throws IOException {
        Path reportPath = Paths.get(path);
        if (reportPath.getParent() != null) {
            Files.createDirectories(reportPath.getParent());
        }
        Files.writeString(reportPath, content, StandardCharsets.UTF_8);
    }

    private static String envOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static boolean envBool(String value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "1", "true", "yes", "on" -> true;
            case "0", "false", "no", "off" -> false;
            default -> fallback;
        };
    }

    private static final class MetricsAccumulator {
        private int totalSamples;
        private int requiredAllCorrect;
        private int lowConfidenceCount;
        private int failures;
        private final Map<String, Integer> fieldCorrect = new LinkedHashMap<>();
        private final Map<String, Integer> fieldTotals = new LinkedHashMap<>();
        private final Map<String, Integer> failurePatterns = new LinkedHashMap<>();
        private final List<Long> durationsMs = new ArrayList<>();

        void record(EvaluationResult result) {
            totalSamples += result.totalSamples;
            requiredAllCorrect += result.requiredAllCorrect;
            for (Map.Entry<String, Integer> entry : result.fieldCorrect.entrySet()) {
                fieldCorrect.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
            for (Map.Entry<String, Integer> entry : result.fieldTotals.entrySet()) {
                fieldTotals.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
            for (Map.Entry<String, Integer> entry : result.failurePatterns.entrySet()) {
                failurePatterns.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
            durationsMs.add(result.elapsedMs);
            if (result.confidence < CONFIDENCE_THRESHOLD) {
                lowConfidenceCount++;
            }
        }

        void recordFailure() {
            failures++;
            totalSamples++;
        }

        String renderSummary() {
            double requiredAccuracy = totalSamples == 0 ? 0 : (double) requiredAllCorrect / totalSamples;
            long median = percentile(50);
            long p95 = percentile(95);
            double lowConfidencePct = totalSamples == 0 ? 0 : (double) lowConfidenceCount / totalSamples;

            String passFailAccuracy = requiredAccuracy >= REQUIRED_ACCURACY_THRESHOLD ? "PASS" : "FAIL";
            String passFailTime = median <= MEDIAN_TIME_THRESHOLD_MS ? "PASS" : "FAIL";

            StringBuilder summary = new StringBuilder();
            summary.append("| Metric | Value |\n");
            summary.append("| --- | --- |\n");
            summary.append("| Required-field accuracy | ")
                    .append(formatPercent(requiredAccuracy))
                    .append(" (")
                    .append(passFailAccuracy)
                    .append(") |\n");
            summary.append("| Median scan-form time | ")
                    .append(median)
                    .append(" ms (")
                    .append(passFailTime)
                    .append(") |\n");
            summary.append("| P95 scan-form time | ").append(p95).append(" ms |\n");
            summary.append("| % scans < 0.70 confidence | ")
                    .append(formatPercent(lowConfidencePct))
                    .append(" |\n");
            summary.append("| Samples | ").append(totalSamples).append(" |\n");
            summary.append("| OCR failures | ").append(failures).append(" |\n");
            return summary.toString();
        }

        String renderFieldTable() {
            StringBuilder table = new StringBuilder();
            table.append("| Field | Accuracy |\n");
            table.append("| --- | --- |\n");
            for (String field : ALL_FIELDS) {
                int total = fieldTotals.getOrDefault(field, 0);
                int correct = fieldCorrect.getOrDefault(field, 0);
                double accuracy = total == 0 ? 0 : (double) correct / total;
                table.append("| ").append(field).append(" | ").append(formatPercent(accuracy)).append(" |\n");
            }
            return table.toString();
        }

        String renderFailurePatterns() {
            if (failurePatterns.isEmpty()) {
                return "No failure patterns detected.\n";
            }
            StringBuilder builder = new StringBuilder();
            builder.append("**Top failure patterns**\n\n");
            builder.append("| Pattern | Count |\n");
            builder.append("| --- | --- |\n");
            failurePatterns.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                    .limit(5)
                    .forEach(entry -> builder.append("| ")
                            .append(entry.getKey())
                            .append(" | ")
                            .append(entry.getValue())
                            .append(" |\n"));
            return builder.toString();
        }

        long percentile(int percentile) {
            if (durationsMs.isEmpty()) {
                return 0;
            }
            List<Long> sorted = new ArrayList<>(durationsMs);
            Collections.sort(sorted);
            int index = (int) Math.ceil((percentile / 100.0) * sorted.size()) - 1;
            index = Math.max(0, Math.min(index, sorted.size() - 1));
            return sorted.get(index);
        }

        String formatPercent(double value) {
            return String.format(Locale.UK, "%.1f%%", value * 100);
        }
    }

    private static final class EvaluationResult {
        int totalSamples;
        int requiredAllCorrect;
        long elapsedMs;
        double confidence;
        Map<String, Integer> fieldCorrect = new LinkedHashMap<>();
        Map<String, Integer> fieldTotals = new LinkedHashMap<>();
        Map<String, Integer> failurePatterns = new LinkedHashMap<>();
    }

    private record GroundTruthSample(
            String bucket,
            Path imageFile,
            GroundTruthFields truth,
            String id
    ) {}

    private record GroundTruth(String image, GroundTruthFields fields) {}

    private record GroundTruthFields(
            String firstName,
            String lastName,
            String dateOfBirth,
            String expiryDate,
            String licenceNumber,
            String addressLine,
            List<String> categories
    ) {}
}
