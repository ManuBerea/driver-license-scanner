package com.dls.driverlicensescannerapi.service;

import com.dls.driverlicensescannerapi.dto.LicenseFields;
import com.dls.driverlicensescannerapi.dto.ScanResponse;
import com.dls.driverlicensescannerapi.error.ErrorCatalog;
import com.dls.driverlicensescannerapi.ocr.OcrClient;
import com.dls.driverlicensescannerapi.ocr.OcrClientException;
import com.dls.driverlicensescannerapi.ocr.OcrResult;
import com.dls.driverlicensescannerapi.parser.LicenseFieldParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ScanService {

    private static final List<String> FALLBACK_ORDER = List.of("paddle", "vision");

    private final OcrClient ocrClient;
    private final ResponseAssembler responseAssembler;
    private final boolean fallbackEnabled;
    private final int maxFallbackAttempts;
    private final double confidenceThreshold;

    public ScanService(
            OcrClient ocrClient,
            ResponseAssembler responseAssembler,
            @Value("${ENABLE_FALLBACK_OCR:false}") boolean fallbackEnabled,
            @Value("${MAX_FALLBACK_ATTEMPTS:2}") int maxFallbackAttempts,
            @Value("${OCR_CONFIDENCE_WARN_THRESHOLD:0.70}") double confidenceThreshold
    ) {
        this.ocrClient = ocrClient;
        this.responseAssembler = responseAssembler;
        this.fallbackEnabled = fallbackEnabled;
        this.maxFallbackAttempts = Math.max(1, maxFallbackAttempts);
        this.confidenceThreshold = confidenceThreshold;
    }

    public ScanResponse scan(MultipartFile image, String requestId) {
        if (!fallbackEnabled) {
            OcrResult ocrResult = ocrClient.scan(image, requestId);
            LicenseFields fields = LicenseFieldParser.parse(ocrResult.lines());
            return responseAssembler.assemble(requestId, ocrResult, fields);
        }

        List<String> attemptedEngines = new ArrayList<>();
        ScanResponse lastResponse = null;
        OcrClientException lastException = null;

        for (String engine : FALLBACK_ORDER) {
            if (attemptedEngines.size() >= maxFallbackAttempts) {
                break;
            }
            String normalizedEngine = engine.toLowerCase(Locale.ROOT);
            try {
                OcrResult ocrResult = ocrClient.scan(image, requestId, normalizedEngine);
                String selectedEngine = ocrResult.engine() == null ? normalizedEngine : ocrResult.engine();
                attemptedEngines.add(normalizedEngine);

                LicenseFields fields = LicenseFieldParser.parse(ocrResult.lines());
                double confidence = FieldConfidenceCalculator.compute(fields);
                boolean missingRequired = FieldConfidenceCalculator.hasMissingRequired(fields);

                lastResponse = responseAssembler.assemble(
                        requestId,
                        ocrResult,
                        fields,
                        List.copyOf(attemptedEngines),
                        selectedEngine
                );

                if (!shouldFallback(confidence, missingRequired)) {
                    return lastResponse;
                }
            } catch (OcrClientException ex) {
                attemptedEngines.add(normalizedEngine);
                lastException = ex;
            }
        }

        if (lastResponse != null) {
            return lastResponse;
        }
        if (lastException != null) {
            throw lastException;
        }
        throw new OcrClientException(ErrorCatalog.OCR_FAILED_CODE, ErrorCatalog.OCR_FAILED_MESSAGE);
    }

    private boolean shouldFallback(double confidence, boolean missingRequired) {
        return confidence < confidenceThreshold || missingRequired;
    }
}
